import { describe, expect, it } from 'vitest'
import {
  ledgerBusinessLabel,
  ledgerIsCharge,
  ledgerMovementLabel,
  ledgerPositionLabel,
  ledgerStageLabel,
} from './billing-ledger'
import type { BillingLedgerEntry } from '@/types/api'

function entry(overrides: Partial<BillingLedgerEntry>): BillingLedgerEntry {
  return {
    id: '1',
    operationType: 'settle',
    availableDelta: 0,
    reservedDelta: -3,
    consumedDelta: 3,
    availableAfter: 94,
    reservedAfter: 0,
    referenceType: 'ai_task',
    referenceId: 'task-1',
    businessOperation: null,
    description: null,
    createdAt: '2026-10-22T11:26:57Z',
    ...overrides,
  }
}

describe('billing ledger presentation', () => {
  it('shows a reservation as frozen points instead of a second available balance deduction', () => {
    const reserve = entry({
      operationType: 'reserve',
      availableDelta: -3,
      reservedDelta: 3,
      consumedDelta: 0,
      reservedAfter: 3,
    })

    expect(ledgerMovementLabel(reserve, 'zh')).toBe('冻结 3')
    expect(ledgerPositionLabel(reserve, 'zh')).toBe('可用 94 · 冻结 3')
    expect(ledgerIsCharge(reserve)).toBe(false)
  })

  it('shows settlement as one consumed charge without deducting available points again', () => {
    const settle = entry({})

    expect(ledgerMovementLabel(settle, 'zh')).toBe('已扣 3')
    expect(ledgerPositionLabel(settle, 'zh')).toBe('可用 94 · 冻结 0')
    expect(ledgerIsCharge(settle)).toBe(true)
  })

  it('keeps grant movements explicit in English', () => {
    const grant = entry({
      operationType: 'trial_grant',
      availableDelta: 100,
      reservedDelta: 0,
      consumedDelta: 0,
      availableAfter: 100,
    })

    expect(ledgerMovementLabel(grant, 'en')).toBe('+100 available')
    expect(ledgerPositionLabel(grant, 'en')).toBe('Available 100 · Frozen 0')
  })

  it('uses the billing task business name for search charges', () => {
    const settle = entry({ businessOperation: 'internet_search' })

    expect(ledgerBusinessLabel(settle, 'zh')).toBe('搜索花费')
    expect(ledgerStageLabel(settle, 'zh')).toBe('扣点')
    expect(ledgerBusinessLabel(settle, 'en')).toBe('Internet search')
  })

  it('uses the point pack name for purchases', () => {
    const purchase = entry({
      operationType: 'purchase_grant',
      referenceType: 'order',
      businessOperation: null,
    })

    expect(ledgerBusinessLabel(purchase, 'zh', '轻量加量包')).toBe('轻量加量包')
    expect(ledgerStageLabel(purchase, 'zh')).toBe('购买到账')
  })
})
