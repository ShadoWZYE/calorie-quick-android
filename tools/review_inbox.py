#!/usr/bin/env python3
"""Windows desktop review inbox for Calorie Quick field-test bundles."""

from __future__ import annotations

import os
import sys
import tkinter as tk
from pathlib import Path
from tkinter import filedialog, messagebox, ttk

from review_inbox_core import (
    CLASSIFICATIONS,
    DECISIONS,
    PROMOTABLE_DECISIONS,
    SEVERITIES,
    STATUSES,
    ReviewInboxStore,
)


class ReviewInboxApp(tk.Tk):
    def __init__(self):
        super().__init__()
        self.title("Calorie Quick · Local Review Inbox")
        self.geometry("1500x900")
        self.minsize(1120, 720)
        self.store = ReviewInboxStore()
        self.reviews: list[dict] = []
        self.review: dict | None = None
        self.item: dict | None = None
        self._loading = False
        self.protocol("WM_DELETE_WINDOW", self.close_app)
        self._build_ui()
        self.reload_reviews()

    def _build_ui(self):
        toolbar = ttk.Frame(self, padding=8)
        toolbar.pack(fill=tk.X)
        ttk.Button(toolbar, text="Import review ZIP…", command=self.import_bundle).pack(side=tk.LEFT, padx=3)
        ttk.Button(toolbar, text="Save review", command=self.save_current).pack(side=tk.LEFT, padx=3)
        ttk.Button(toolbar, text="Add manual item", command=self.add_manual_item).pack(side=tk.LEFT, padx=3)
        ttk.Button(toolbar, text="Export implementation brief…", command=self.export_brief).pack(side=tk.LEFT, padx=3)
        ttk.Button(toolbar, text="Open private inbox folder", command=self.open_inbox_folder).pack(side=tk.RIGHT, padx=3)

        self.status_text = tk.StringVar(value="Ready")
        ttk.Label(self, textvariable=self.status_text, padding=(10, 3)).pack(side=tk.BOTTOM, fill=tk.X)

        panes = ttk.Panedwindow(self, orient=tk.HORIZONTAL)
        panes.pack(fill=tk.BOTH, expand=True, padx=8, pady=(0, 8))

        bundle_frame = ttk.LabelFrame(panes, text="Imported bundles", padding=6)
        panes.add(bundle_frame, weight=1)
        self.bundle_list = tk.Listbox(bundle_frame, exportselection=False, width=27)
        bundle_scroll = ttk.Scrollbar(bundle_frame, orient=tk.VERTICAL, command=self.bundle_list.yview)
        self.bundle_list.configure(yscrollcommand=bundle_scroll.set)
        self.bundle_list.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)
        bundle_scroll.pack(side=tk.RIGHT, fill=tk.Y)
        self.bundle_list.bind("<<ListboxSelect>>", self.select_bundle)

        item_frame = ttk.LabelFrame(panes, text="Review items", padding=6)
        panes.add(item_frame, weight=3)
        filter_row = ttk.Frame(item_frame)
        filter_row.pack(fill=tk.X, pady=(0, 5))
        ttk.Label(filter_row, text="Show:").pack(side=tk.LEFT)
        self.filter_value = tk.StringVar(value="All")
        filter_box = ttk.Combobox(
            filter_row,
            textvariable=self.filter_value,
            values=["All", "Needs decision", "Accepted", "Promoted", "Unresolved"],
            state="readonly",
            width=18,
        )
        filter_box.pack(side=tk.LEFT, padx=5)
        filter_box.bind("<<ComboboxSelected>>", lambda _event: self.refresh_items())
        columns = ("source", "classification", "severity", "decision")
        self.item_tree = ttk.Treeview(item_frame, columns=columns, show="tree headings", selectmode="browse")
        self.item_tree.heading("#0", text="Title")
        self.item_tree.heading("source", text="Source")
        self.item_tree.heading("classification", text="Class")
        self.item_tree.heading("severity", text="Severity")
        self.item_tree.heading("decision", text="Decision")
        self.item_tree.column("#0", width=290, minwidth=180)
        self.item_tree.column("source", width=105, minwidth=85)
        self.item_tree.column("classification", width=95, minwidth=75)
        self.item_tree.column("severity", width=80, minwidth=65)
        self.item_tree.column("decision", width=150, minwidth=100)
        item_scroll = ttk.Scrollbar(item_frame, orient=tk.VERTICAL, command=self.item_tree.yview)
        self.item_tree.configure(yscrollcommand=item_scroll.set)
        self.item_tree.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)
        item_scroll.pack(side=tk.RIGHT, fill=tk.Y)
        self.item_tree.bind("<<TreeviewSelect>>", self.select_item)

        editor_outer = ttk.LabelFrame(panes, text="Triage and reviewer additions", padding=6)
        panes.add(editor_outer, weight=4)
        editor_canvas = tk.Canvas(editor_outer, highlightthickness=0)
        editor_scroll = ttk.Scrollbar(editor_outer, orient=tk.VERTICAL, command=editor_canvas.yview)
        self.editor = ttk.Frame(editor_canvas, padding=(5, 2, 10, 10))
        editor_window = editor_canvas.create_window((0, 0), window=self.editor, anchor="nw")
        editor_canvas.configure(yscrollcommand=editor_scroll.set)
        editor_canvas.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)
        editor_scroll.pack(side=tk.RIGHT, fill=tk.Y)
        self.editor.bind("<Configure>", lambda _event: editor_canvas.configure(scrollregion=editor_canvas.bbox("all")))
        editor_canvas.bind("<Configure>", lambda event: editor_canvas.itemconfigure(editor_window, width=event.width))
        self._build_editor()

    def _build_editor(self):
        self.title_value = tk.StringVar()
        self.classification_value = tk.StringVar()
        self.severity_value = tk.StringVar()
        self.status_value = tk.StringVar()
        self.decision_value = tk.StringVar()
        self.target_value = tk.StringVar()
        self.reference_value = tk.StringVar()
        self.promote_value = tk.BooleanVar()
        self.suggestion_value = tk.StringVar()

        ttk.Label(self.editor, text="Title").grid(row=0, column=0, sticky="w")
        ttk.Entry(self.editor, textvariable=self.title_value).grid(row=1, column=0, columnspan=4, sticky="ew", pady=(0, 7))
        ttk.Label(self.editor, textvariable=self.suggestion_value, foreground="#555555").grid(
            row=2, column=0, columnspan=4, sticky="w", pady=(0, 7),
        )

        fields = [
            ("Classification", self.classification_value, CLASSIFICATIONS),
            ("Severity", self.severity_value, SEVERITIES),
            ("Status", self.status_value, STATUSES),
            ("Decision", self.decision_value, DECISIONS),
        ]
        for column, (label, variable, values) in enumerate(fields):
            ttk.Label(self.editor, text=label).grid(row=3, column=column, sticky="w", padx=(0, 5))
            ttk.Combobox(self.editor, textvariable=variable, values=values, state="readonly").grid(
                row=4, column=column, sticky="ew", padx=(0, 5), pady=(0, 7),
            )

        ttk.Label(self.editor, text="Target version").grid(row=5, column=0, sticky="w")
        ttk.Entry(self.editor, textvariable=self.target_value).grid(row=6, column=0, columnspan=2, sticky="ew", padx=(0, 5))
        ttk.Label(self.editor, text="Issue / commit / reference").grid(row=5, column=2, sticky="w")
        ttk.Entry(self.editor, textvariable=self.reference_value).grid(row=6, column=2, columnspan=2, sticky="ew")

        self.promote_check = ttk.Checkbutton(
            self.editor,
            text="Promote to the next generated implementation brief",
            variable=self.promote_value,
        )
        self.promote_check.grid(row=7, column=0, columnspan=4, sticky="w", pady=8)

        self.description_text = self._text_field(8, "Original evidence / manual request", height=5)
        self.analysis_text = self._text_field(10, "Codex analysis", height=6)
        self.rationale_text = self._text_field(12, "Decision rationale", height=4)
        self.comments_text = self._text_field(14, "Reviewer comments", height=5)
        self.corrections_text = self._text_field(16, "Corrections or clarified expected behavior", height=5)
        self.implementation_text = self._text_field(18, "Implementation notes and acceptance checks", height=6)

        attachment_row = ttk.Frame(self.editor)
        attachment_row.grid(row=20, column=0, columnspan=4, sticky="ew", pady=(8, 0))
        self.attachment_label = ttk.Label(attachment_row, text="No attachment")
        self.attachment_label.pack(side=tk.LEFT, fill=tk.X, expand=True)
        self.open_attachment_button = ttk.Button(attachment_row, text="Open attachment", command=self.open_attachment)
        self.open_attachment_button.pack(side=tk.RIGHT)
        ttk.Button(self.editor, text="Save this item", command=self.save_current).grid(
            row=21, column=0, columnspan=4, sticky="ew", pady=(10, 0),
        )
        for column in range(4):
            self.editor.columnconfigure(column, weight=1)

    def _text_field(self, row: int, label: str, height: int) -> tk.Text:
        ttk.Label(self.editor, text=label).grid(row=row, column=0, columnspan=4, sticky="w", pady=(5, 0))
        text = tk.Text(self.editor, height=height, wrap=tk.WORD, undo=True)
        text.grid(row=row + 1, column=0, columnspan=4, sticky="nsew")
        return text

    def reload_reviews(self, select_hash: str | None = None):
        self.reviews = self.store.list_reviews()
        self.bundle_list.delete(0, tk.END)
        selection = None
        for index, review in enumerate(self.reviews):
            build = review.get("build") or {}
            version = build.get("versionName") or "unknown build"
            label = f"{version} · {review.get('bundleHash', '')[:10]}\n{len(review.get('items', []))} items"
            self.bundle_list.insert(tk.END, label)
            if review.get("bundleHash") == select_hash:
                selection = index
        if self.reviews:
            index = selection if selection is not None else 0
            self.bundle_list.selection_set(index)
            self.bundle_list.activate(index)
            self._load_review(index)
        else:
            self.review = None
            self.refresh_items()

    def import_bundle(self):
        path = filedialog.askopenfilename(title="Import Calorie Quick review ZIP", filetypes=[("ZIP archives", "*.zip")])
        if not path:
            return
        self.status_text.set("Validating and importing bundle…")
        self.update_idletasks()
        try:
            review, created = self.store.import_bundle(Path(path))
            self.reload_reviews(review["bundleHash"])
            self.status_text.set("Imported new bundle" if created else "Bundle already existed; opened saved review")
        except Exception as exc:
            self.status_text.set("Import failed")
            messagebox.showerror("Cannot import bundle", str(exc))

    def select_bundle(self, _event=None):
        selected = self.bundle_list.curselection()
        if not selected:
            return
        self.save_current(silent=True)
        self._load_review(selected[0])

    def _load_review(self, index: int):
        self.review = self.reviews[index]
        self.item = None
        self.refresh_items()
        build = self.review.get("build") or {}
        self.status_text.set(
            f"{build.get('versionName', 'Unknown build')} · {self.review.get('bundleHash', '')[:12]} · "
            f"private data: {self.store.root}",
        )

    def _visible_items(self) -> list[dict]:
        items = list((self.review or {}).get("items", []))
        selected_filter = self.filter_value.get()
        if selected_filter == "Needs decision":
            return [item for item in items if item.get("decision") == "UNDECIDED"]
        if selected_filter == "Accepted":
            return [item for item in items if item.get("decision") in PROMOTABLE_DECISIONS]
        if selected_filter == "Promoted":
            return [item for item in items if item.get("promote")]
        if selected_filter == "Unresolved":
            return [item for item in items if item.get("status") not in {"RESOLVED", "CLOSED"}]
        return items

    def refresh_items(self, select_id: str | None = None):
        self.item_tree.delete(*self.item_tree.get_children())
        for item in self._visible_items():
            prefix = "★ " if item.get("promote") else ""
            self.item_tree.insert(
                "", tk.END, iid=item["id"], text=prefix + item.get("title", "Untitled"),
                values=(item.get("sourceType"), item.get("classification"), item.get("severity"), item.get("decision")),
            )
        target = select_id if select_id in self.item_tree.get_children() else None
        if target is None and self.item_tree.get_children():
            target = self.item_tree.get_children()[0]
        if target:
            self.item_tree.selection_set(target)
            self.item_tree.focus(target)
            self._load_item(target)
        else:
            self.item = None
            self._clear_editor()

    def select_item(self, _event=None):
        selected = self.item_tree.selection()
        if not selected:
            return
        self.save_editor()
        self._load_item(selected[0])

    def _load_item(self, item_id: str):
        self.item = next((item for item in (self.review or {}).get("items", []) if item.get("id") == item_id), None)
        if not self.item:
            return
        self._loading = True
        item = self.item
        self.title_value.set(item.get("title", ""))
        self.classification_value.set(item.get("classification", "FEATURE"))
        self.severity_value.set(item.get("severity", "LOW"))
        self.status_value.set(item.get("status", "NEW"))
        self.decision_value.set(item.get("decision", "UNDECIDED"))
        self.target_value.set(item.get("targetVersion", ""))
        self.reference_value.set(item.get("linkedReference", ""))
        self.promote_value.set(bool(item.get("promote")))
        self.suggestion_value.set(
            f"Automatic suggestion: {item.get('suggestedClassification')} / {item.get('suggestedSeverity')} · "
            f"Source: {item.get('sourceType')}",
        )
        self._set_text(self.description_text, item.get("description", ""))
        self.description_text.configure(state=tk.NORMAL if item.get("sourceType") == "MANUAL" else tk.DISABLED)
        self._set_text(self.analysis_text, item.get("assistantAnalysis", ""))
        self.analysis_text.configure(state=tk.DISABLED)
        self._set_text(self.rationale_text, item.get("rationale", ""))
        self._set_text(self.comments_text, item.get("reviewerComments", ""))
        self._set_text(self.corrections_text, item.get("corrections", ""))
        self._set_text(self.implementation_text, item.get("implementationNotes", ""))
        attachments = item.get("attachments", [])
        self.attachment_label.configure(text=Path(attachments[0]).name if attachments else "No attachment")
        self.open_attachment_button.configure(state=tk.NORMAL if attachments else tk.DISABLED)
        self._loading = False

    def _clear_editor(self):
        self._loading = True
        for variable in (
            self.title_value, self.classification_value, self.severity_value, self.status_value,
            self.decision_value, self.target_value, self.reference_value, self.suggestion_value,
        ):
            variable.set("")
        self.promote_value.set(False)
        for widget in (
            self.description_text, self.analysis_text, self.rationale_text, self.comments_text,
            self.corrections_text, self.implementation_text,
        ):
            widget.configure(state=tk.NORMAL)
            self._set_text(widget, "")
        self.attachment_label.configure(text="No attachment")
        self.open_attachment_button.configure(state=tk.DISABLED)
        self._loading = False

    @staticmethod
    def _set_text(widget: tk.Text, value: str):
        previous = str(widget.cget("state"))
        widget.configure(state=tk.NORMAL)
        widget.delete("1.0", tk.END)
        widget.insert("1.0", value)
        widget.configure(state=previous)

    @staticmethod
    def _get_text(widget: tk.Text) -> str:
        return widget.get("1.0", tk.END).strip()

    def save_editor(self):
        if self._loading or not self.item:
            return
        self.item.update({
            "title": self.title_value.get().strip() or "Untitled",
            "classification": self.classification_value.get(),
            "severity": self.severity_value.get(),
            "status": self.status_value.get(),
            "decision": self.decision_value.get(),
            "targetVersion": self.target_value.get().strip(),
            "linkedReference": self.reference_value.get().strip(),
            "promote": bool(self.promote_value.get()),
            "rationale": self._get_text(self.rationale_text),
            "reviewerComments": self._get_text(self.comments_text),
            "corrections": self._get_text(self.corrections_text),
            "implementationNotes": self._get_text(self.implementation_text),
        })
        if self.item.get("sourceType") == "MANUAL":
            self.item["description"] = self._get_text(self.description_text)

    def save_current(self, silent: bool = False):
        if not self.review:
            return
        self.save_editor()
        self.store.save_review(self.review)
        selected_id = self.item.get("id") if self.item else None
        self.refresh_items(selected_id)
        if not silent:
            self.status_text.set("Review saved locally")

    def add_manual_item(self):
        if not self.review:
            messagebox.showinfo("Import a bundle", "Import or select a review bundle before adding a manual item.")
            return
        self.save_current(silent=True)
        item = self.store.add_manual_item(self.review)
        self.filter_value.set("All")
        self.refresh_items(item["id"])

    def open_attachment(self):
        attachments = (self.item or {}).get("attachments", [])
        if not attachments:
            return
        path = Path(attachments[0])
        if not path.is_file():
            messagebox.showerror("Attachment missing", str(path))
            return
        os.startfile(path)

    def open_inbox_folder(self):
        self.store.root.mkdir(parents=True, exist_ok=True)
        os.startfile(self.store.root)

    def export_brief(self):
        self.save_current(silent=True)
        path = filedialog.asksaveasfilename(
            title="Export implementation brief",
            defaultextension=".json",
            initialfile="calorie-quick-implementation-brief.json",
            filetypes=[("JSON and Markdown brief", "*.json")],
        )
        if not path:
            return
        try:
            brief = self.store.export_implementation_brief(self.reviews, Path(path))
            self.status_text.set(f"Exported {brief['itemCount']} promoted implementation item(s)")
            messagebox.showinfo(
                "Implementation brief created",
                f"Created JSON and Markdown briefs with {brief['itemCount']} explicitly promoted item(s).",
            )
        except Exception as exc:
            messagebox.showerror("Cannot export brief", str(exc))

    def close_app(self):
        try:
            self.save_current(silent=True)
        finally:
            self.destroy()


def main() -> int:
    try:
        ReviewInboxApp().mainloop()
        return 0
    except tk.TclError as exc:
        print(f"Cannot open Windows review inbox: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
