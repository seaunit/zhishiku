import type { BillingLedgerEntry } from '@/types/api'

const BUSINESS_OPERATION_LABELS: Record<string, readonly [string, string]> = {
  manual_auto_classify: ['自动分类花费', 'Automatic classification'],
  internet_import: ['互联网内容整理花费', 'Web result organization'],
  text_import: ['AI 文字整理花费', 'AI text organization'],
  reanalyze: ['重新整理花费', 'Knowledge reanalysis'],
  webpage_import: ['网页采集花费', 'Web page capture'],
  document_import: ['文档采集花费', 'Document capture'],
  screenshot_import: ['截图采集花费', 'Screenshot capture'],
  internet_search: ['搜索花费', 'Internet search'],
  internet_search_with_answer: ['搜索与回答花费', 'Search and answer'],
  knowledge_chat: ['知识库回答花费', 'Knowledge chat answer'],
}

function signed(value: number) {
  return value > 0 ? `+${value}` : String(value)
}

function fallbackDelta(entry: BillingLedgerEntry) {
  if (entry.availableDelta !== 0) return entry.availableDelta
  if (entry.reservedDelta !== 0) return entry.reservedDelta
  return entry.consumedDelta
}

export function ledgerBusinessLabel(
  entry: BillingLedgerEntry,
  locale: string,
  packName?: string,
) {
  const en = locale === 'en'
  if (entry.businessOperation) {
    const label = BUSINESS_OPERATION_LABELS[entry.businessOperation]
    if (label) return en ? label[1] : label[0]
  }
  switch (entry.operationType) {
    case 'purchase_grant':
      return packName || (en ? 'Point pack purchase' : '点数套餐购买')
    case 'trial_grant':
      return en ? 'Trial points' : '体验点赠送'
    case 'refund_recover':
      return en ? 'Refund recovery' : '退款回收'
    case 'manual_adjustment':
      return en ? 'Manual adjustment' : '人工调整'
    case 'reserve':
    case 'reserve_release':
    case 'settle':
      return en ? 'Point activity' : '点数任务'
    default:
      return en ? 'Point activity' : '点数变动'
  }
}

export function ledgerStageLabel(entry: BillingLedgerEntry, locale: string) {
  const en = locale === 'en'
  const labels: Record<string, readonly [string, string]> = {
    trial_grant: ['到账', 'Granted'],
    purchase_grant: ['购买到账', 'Purchase credited'],
    reserve: ['预留', 'Reserved'],
    reserve_release: ['释放', 'Released'],
    settle: ['扣点', 'Charged'],
    refund_recover: ['退款回收', 'Refund recovery'],
    manual_adjustment: ['人工调整', 'Manual adjustment'],
  }
  const label = labels[entry.operationType]
  return label ? (en ? label[1] : label[0]) : entry.operationType
}

export function ledgerMovementLabel(entry: BillingLedgerEntry, locale: string) {
  const en = locale === 'en'
  switch (entry.operationType) {
    case 'trial_grant':
    case 'purchase_grant':
      return en ? `${signed(entry.availableDelta)} available` : `${signed(entry.availableDelta)} 可用`
    case 'reserve':
      return en ? `Frozen ${entry.reservedDelta}` : `冻结 ${entry.reservedDelta}`
    case 'reserve_release':
      return en
        ? `Released ${signed(entry.availableDelta)}`
        : `释放 ${signed(entry.availableDelta)}`
    case 'settle':
      return en ? `Charged ${entry.consumedDelta}` : `已扣 ${entry.consumedDelta}`
    case 'refund_recover':
      return en
        ? `Recovered ${signed(entry.availableDelta)}`
        : `回收 ${signed(entry.availableDelta)}`
    default:
      return signed(fallbackDelta(entry))
  }
}

export function ledgerPositionLabel(entry: BillingLedgerEntry, locale: string) {
  return locale === 'en'
    ? `Available ${entry.availableAfter} · Frozen ${entry.reservedAfter}`
    : `可用 ${entry.availableAfter} · 冻结 ${entry.reservedAfter}`
}

export function ledgerIsCharge(entry: BillingLedgerEntry) {
  if (entry.operationType === 'settle') return entry.consumedDelta > 0
  return (
    entry.operationType === 'refund_recover' ||
    (entry.operationType === 'manual_adjustment' && fallbackDelta(entry) < 0)
  )
}
