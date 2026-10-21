import type { RichTextNode } from '@/types/rich-text'

function cell(type: 'tableHeader' | 'tableCell'): RichTextNode {
  return {
    type,
    content: [{ type: 'paragraph' }],
  }
}

function row(type: 'tableHeaderRow' | 'tableBodyRow', columns: number): RichTextNode {
  return {
    type,
    content: Array.from({ length: columns }, () =>
      cell(type === 'tableHeaderRow' ? 'tableHeader' : 'tableCell'),
    ),
  }
}

export function createTableNode(rows = 3, columns = 3): RichTextNode {
  const safeRows = Math.max(1, Math.min(50, rows))
  const safeColumns = Math.max(1, Math.min(20, columns))
  return {
    type: 'table',
    attrs: { headerRow: true },
    content: [
      row('tableHeaderRow', safeColumns),
      ...Array.from({ length: Math.max(0, safeRows - 1) }, () =>
        row('tableBodyRow', safeColumns),
      ),
    ],
  }
}

export function tableDimensions(table: RichTextNode): {
  rows: number
  columns: number
} {
  const rows = table.content?.length || 0
  return {
    rows,
    columns: table.content?.[0]?.content?.length || 0,
  }
}

export function addTableRow(
  table: RichTextNode,
  rowIndex: number,
): RichTextNode {
  const dimensions = tableDimensions(table)
  const rows = [...(table.content || [])]
  const insertAt = Math.max(1, Math.min(rows.length, rowIndex + 1))
  rows.splice(insertAt, 0, row('tableBodyRow', Math.max(1, dimensions.columns)))
  return { ...table, content: rows }
}

export function deleteTableRow(
  table: RichTextNode,
  rowIndex: number,
): RichTextNode {
  const rows = [...(table.content || [])]
  if (rows.length <= 1 || rowIndex <= 0 || rowIndex >= rows.length) {
    return table
  }
  rows.splice(rowIndex, 1)
  return { ...table, content: rows }
}

export function addTableColumn(
  table: RichTextNode,
  columnIndex: number,
): RichTextNode {
  return {
    ...table,
    content: table.content?.map((tableRow) => {
      const cells = [...(tableRow.content || [])]
      const insertAt = Math.max(0, Math.min(cells.length, columnIndex + 1))
      cells.splice(
        insertAt,
        0,
        cell(tableRow.type === 'tableHeaderRow' ? 'tableHeader' : 'tableCell'),
      )
      return { ...tableRow, content: cells.slice(0, 20) }
    }),
  }
}

export function deleteTableColumn(
  table: RichTextNode,
  columnIndex: number,
): RichTextNode {
  const dimensions = tableDimensions(table)
  if (dimensions.columns <= 1 || columnIndex < 0 || columnIndex >= dimensions.columns) {
    return table
  }
  return {
    ...table,
    content: table.content?.map((tableRow) => ({
      ...tableRow,
      content: (tableRow.content || []).filter((_, index) => index !== columnIndex),
    })),
  }
}
