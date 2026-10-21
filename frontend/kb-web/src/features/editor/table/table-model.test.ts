import { describe, expect, it } from 'vitest'
import {
  addTableColumn,
  addTableRow,
  createTableNode,
  deleteTableColumn,
  deleteTableRow,
  tableDimensions,
} from './table-model'

describe('rich table model', () => {
  it('creates a three-by-three table with a semantic header row', () => {
    const table = createTableNode(3, 3)

    expect(table.attrs).toEqual({ headerRow: true })
    expect(table.content?.map((row) => row.type)).toEqual([
      'tableHeaderRow',
      'tableBodyRow',
      'tableBodyRow',
    ])
    expect(tableDimensions(table)).toEqual({ rows: 3, columns: 3 })
  })

  it('adds and removes rows without leaving ragged columns', () => {
    const table = addTableRow(createTableNode(2, 2), 1)
    expect(tableDimensions(table)).toEqual({ rows: 3, columns: 2 })

    const removed = deleteTableRow(table, 1)
    expect(tableDimensions(removed)).toEqual({ rows: 2, columns: 2 })
  })

  it('adds and removes whole columns from every row', () => {
    const table = addTableColumn(createTableNode(2, 2), 0)
    expect(tableDimensions(table)).toEqual({ rows: 2, columns: 3 })

    const removed = deleteTableColumn(table, 2)
    expect(tableDimensions(removed)).toEqual({ rows: 2, columns: 2 })
  })
})
