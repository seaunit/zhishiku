import type { BillingLedgerEntry } from '@/types/api'

function signed(value: number) {
  return value > 0 ? `+${value}` : String(value)
}

function fallbackDelta(entry: BillingLedgerEntry) {
  if (entry.availableDelta !== 0) return entry.availableDelta
  if (entry.reservedDelta !== 0) return entry.reservedDelta
  return entry.consumedDelta
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
