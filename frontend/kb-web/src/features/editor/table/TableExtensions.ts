import { mergeAttributes, Node } from '@tiptap/core'

export const RichTable = Node.create({
  name: 'table',
  group: 'block',
  content: 'tableHeaderRow tableBodyRow*',
  isolating: true,
  defining: true,

  addAttributes() {
    return {
      headerRow: {
        default: true,
        parseHTML: () => true,
        renderHTML: () => ({ 'data-header-row': 'true' }),
      },
    }
  },

  parseHTML() {
    return [{ tag: 'table[data-rich-table]' }]
  },

  renderHTML({ HTMLAttributes }) {
    return [
      'table',
      mergeAttributes(HTMLAttributes, {
        'data-rich-table': 'true',
        class: 'rich-table',
      }),
      0,
    ]
  },
})

export const RichTableHeaderRow = Node.create({
  name: 'tableHeaderRow',
  content: 'tableHeader+',
  isolating: true,
  defining: true,

  parseHTML() {
    return [{ tag: 'tr[data-header-row]' }]
  },

  renderHTML() {
    return ['tr', { 'data-header-row': 'true' }, 0]
  },
})

export const RichTableBodyRow = Node.create({
  name: 'tableBodyRow',
  content: 'tableCell+',
  isolating: true,
  defining: true,

  parseHTML() {
    return [{ tag: 'tr[data-body-row]' }]
  },

  renderHTML() {
    return ['tr', { 'data-body-row': 'true' }, 0]
  },
})

export const RichTableHeader = Node.create({
  name: 'tableHeader',
  content: 'block+',
  isolating: true,
  defining: true,

  addAttributes() {
    return {
      colspan: {
        default: 1,
        parseHTML: (element) => Number(element.getAttribute('colspan') || 1),
        renderHTML: (attributes) =>
          Number(attributes.colspan) > 1 ? { colspan: attributes.colspan } : {},
      },
      rowspan: {
        default: 1,
        parseHTML: (element) => Number(element.getAttribute('rowspan') || 1),
        renderHTML: (attributes) =>
          Number(attributes.rowspan) > 1 ? { rowspan: attributes.rowspan } : {},
      },
    }
  },

  parseHTML() {
    return [{ tag: 'th' }]
  },

  renderHTML({ HTMLAttributes }) {
    return ['th', mergeAttributes(HTMLAttributes), 0]
  },
})

export const RichTableCell = Node.create({
  name: 'tableCell',
  content: 'block+',
  isolating: true,
  defining: true,

  addAttributes() {
    return {
      colspan: {
        default: 1,
        parseHTML: (element) => Number(element.getAttribute('colspan') || 1),
        renderHTML: (attributes) =>
          Number(attributes.colspan) > 1 ? { colspan: attributes.colspan } : {},
      },
      rowspan: {
        default: 1,
        parseHTML: (element) => Number(element.getAttribute('rowspan') || 1),
        renderHTML: (attributes) =>
          Number(attributes.rowspan) > 1 ? { rowspan: attributes.rowspan } : {},
      },
    }
  },

  parseHTML() {
    return [{ tag: 'td' }]
  },

  renderHTML({ HTMLAttributes }) {
    return ['td', mergeAttributes(HTMLAttributes), 0]
  },
})

export const RichTableExtensions = [
  RichTable,
  RichTableHeaderRow,
  RichTableBodyRow,
  RichTableHeader,
  RichTableCell,
]
