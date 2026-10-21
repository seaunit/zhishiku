import approvedSchema from '../../../../../../specs/004-rich-text-editor/contracts/rich-text.schema.json'
import type {
  RichTextDocument,
  RichTextMark,
  RichTextNode,
} from '@/types/rich-text'

export const RICH_TEXT_SCHEMA_VERSION = 1 as const
export const RICH_TEXT_CONTRACT_ID =
  'https://seaunit.site/kb/contracts/rich-text.schema.json'

export const RICH_TEXT_SCHEMA = approvedSchema

export interface RichTextDocumentDto {
  schemaVersion: typeof RICH_TEXT_SCHEMA_VERSION
  document: RichTextDocument
}

export function isSupportedRichTextSchemaVersion(
  value: unknown,
): value is typeof RICH_TEXT_SCHEMA_VERSION {
  return value === RICH_TEXT_SCHEMA_VERSION
}

export function toRichTextDocumentDto(
  document: RichTextDocument,
): RichTextDocumentDto {
  return {
    schemaVersion: RICH_TEXT_SCHEMA_VERSION,
    document,
  }
}

export function fromRichTextDocumentDto(
  dto: RichTextDocumentDto,
): RichTextDocument {
  if (!isSupportedRichTextSchemaVersion(dto.schemaVersion)) {
    throw new Error(`Unsupported rich text schema version: ${String(dto.schemaVersion)}`)
  }
  return dto.document
}

export function contractFixture(): RichTextDocument {
  const marks: RichTextMark[] = [
    { type: 'bold' },
    { type: 'italic' },
    { type: 'underline' },
    { type: 'strike' },
    { type: 'code' },
    {
      type: 'link',
      attrs: { href: 'https://example.com/reference' },
    },
  ]
  const paragraph: RichTextNode = {
    type: 'paragraph',
    content: [{ type: 'text', text: '契约夹具', marks }],
  }

  return {
    type: 'doc',
    content: [
      {
        type: 'heading',
        attrs: { level: 2 },
        content: [{ type: 'text', text: '识海' }],
      },
      paragraph,
    ],
  }
}
