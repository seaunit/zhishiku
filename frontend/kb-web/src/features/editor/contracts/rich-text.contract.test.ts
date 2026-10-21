import { describe, expect, it } from 'vitest'
import {
  RICH_TEXT_CONTRACT_ID,
  RICH_TEXT_SCHEMA,
  RICH_TEXT_SCHEMA_VERSION,
  contractFixture,
  fromRichTextDocumentDto,
  isSupportedRichTextSchemaVersion,
  toRichTextDocumentDto,
} from './index'

describe('rich text contract adapter', () => {
  it('reuses the approved JSON schema instead of copying it', () => {
    expect(RICH_TEXT_SCHEMA.$id).toBe(RICH_TEXT_CONTRACT_ID)
    expect(RICH_TEXT_SCHEMA.title).toContain('富文本')
    expect(RICH_TEXT_SCHEMA.$defs.image.properties.attrs.required).toContain('assetId')
    expect(RICH_TEXT_SCHEMA.$defs.table.required).toContain('content')
  })

  it('maps document DTOs with the current schema version', () => {
    const document = contractFixture()
    const dto = toRichTextDocumentDto(document)

    expect(dto).toEqual({
      schemaVersion: RICH_TEXT_SCHEMA_VERSION,
      document,
    })
    expect(fromRichTextDocumentDto(dto)).toBe(document)
  })

  it('accepts only known schema versions', () => {
    expect(isSupportedRichTextSchemaVersion(RICH_TEXT_SCHEMA_VERSION)).toBe(true)
    expect(isSupportedRichTextSchemaVersion(RICH_TEXT_SCHEMA_VERSION + 1)).toBe(false)
  })
})
