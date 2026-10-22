<template>
  <section class="page billing-page">
    <header class="page-heading billing-heading">
      <div>
        <h1>{{ tx('点数与支付', 'Points and billing') }}</h1>
        <p>{{ tx('一次性点数包，按实际成功的 AI 步骤结算。', 'One-time point packs, settled by successful AI steps.') }}</p>
      </div>
      <a
        v-if="catalog"
        class="button secondary"
        :href="catalog.refundPolicy.supportUrl"
        target="_blank"
        rel="noopener noreferrer"
      >
        <LifeBuoy :size="17" />
        <span>{{ tx('退款支持', 'Refund support') }}</span>
      </a>
    </header>

    <div v-if="loading" class="editor-loading" aria-busy="true">
      <span class="skeleton title-line" />
      <span class="skeleton editor-block" />
    </div>
    <div v-else-if="error" class="inline-error" role="alert">
      <CircleAlert :size="20" />
      <div>
        <strong>{{ tx('计费信息加载失败', 'Billing data could not be loaded') }}</strong>
        <p>{{ error }}</p>
      </div>
      <button type="button" class="text-button" @click="load">{{ t('common.retry') }}</button>
    </div>

    <template v-else>
      <section class="billing-balance panel" aria-labelledby="billing-balance-title">
        <header class="panel-heading compact">
          <div>
            <h2 id="billing-balance-title">{{ tx('点数余额', 'Point balance') }}</h2>
            <p>{{ tx('体验点优先，购买点按最早批次消费。', 'Trial points are used first, then the oldest purchased batch.') }}</p>
          </div>
          <span class="billing-balance-value num">{{ account?.availablePoints ?? 0 }}</span>
        </header>
        <dl class="billing-metrics">
          <div>
            <dt>{{ tx('体验点', 'Trial') }}</dt>
            <dd class="num">{{ account?.trialPoints ?? 0 }}</dd>
          </div>
          <div>
            <dt>{{ tx('购买点', 'Purchased') }}</dt>
            <dd class="num">{{ account?.purchasedPoints ?? 0 }}</dd>
          </div>
          <div>
            <dt>{{ tx('已预留', 'Reserved') }}</dt>
            <dd class="num">{{ account?.reservedPoints ?? 0 }}</dd>
          </div>
        </dl>
      </section>

      <section class="billing-packs" aria-labelledby="billing-packs-title">
        <header class="section-heading">
          <h2 id="billing-packs-title">{{ tx('点数包', 'Point packs') }}</h2>
          <span>{{ tx('一次性购买，点数不可提现或转让', 'One-time purchase. Points cannot be withdrawn or transferred.') }}</span>
        </header>
        <div v-if="catalog && !catalog.billingEnabled" class="billing-return panel" role="status">
          <div>
            <strong>{{ tx('支付通道暂不可用', 'Payment is temporarily unavailable') }}</strong>
            <p>{{ tx('支付通道暂不可用，现有知识、搜索和手动录入不受影响。', 'Payment is temporarily unavailable. Your existing knowledge, search, and manual capture remain available.') }}</p>
          </div>
        </div>
        <article
          v-for="pack in catalog?.packs"
          :key="pack.sku"
          class="billing-pack"
        >
          <span class="billing-pack-name">{{ locale === 'en' ? pack.nameEn : pack.nameZh }}</span>
          <strong class="billing-pack-price num">¥{{ money(pack.priceMinor) }}</strong>
          <span class="billing-pack-points num">{{ pack.points.toLocaleString() }} {{ tx('点', 'points') }}</span>
          <span class="billing-pack-rate">
            {{ (pack.points / (pack.priceMinor / 100)).toFixed(1) }} {{ tx('点/元', 'points/CNY') }}
          </span>
          <button
            type="button"
            class="button primary"
            :disabled="!catalog?.billingEnabled || purchasingSku === pack.sku"
            @click="buy(pack)"
          >
            <LoaderCircle v-if="purchasingSku === pack.sku" class="spin" :size="17" />
            <CreditCard v-else :size="17" />
            <span>{{ tx('购买', 'Buy') }}</span>
          </button>
        </article>
      </section>

      <section v-if="activeOrder" class="billing-return panel" aria-labelledby="billing-return-title">
        <header class="panel-heading compact">
          <div>
            <h2 id="billing-return-title">{{ tx('支付状态', 'Payment status') }}</h2>
            <p>{{ activeOrder.sku }} · ¥{{ money(activeOrder.amountMinor) }} · {{ activeOrder.points }} {{ tx('点', 'points') }}</p>
          </div>
          <span class="status-pill" :class="orderTone(activeOrder.status)">
            {{ orderStatus(activeOrder.status) }}
          </span>
        </header>
        <div class="billing-order-actions">
          <a
            v-if="activeOrder.checkoutUrl && activeOrder.status === 'PENDING'"
            class="button secondary"
            :href="activeOrder.checkoutUrl"
            target="_blank"
            rel="noopener noreferrer"
          >
            {{ tx('返回收银台', 'Return to checkout') }}
          </a>
          <button type="button" class="text-button" @click="refreshOrder">
            {{ tx('刷新状态', 'Refresh status') }}
          </button>
        </div>
      </section>

      <div class="billing-columns">
        <section class="panel" aria-labelledby="billing-orders-title">
          <header class="panel-heading compact">
            <div>
              <h2 id="billing-orders-title">{{ tx('订单历史', 'Order history') }}</h2>
              <p>{{ tx('可靠支付结果到达后自动到账。', 'Points arrive after a reliable payment result.') }}</p>
            </div>
          </header>
          <div class="billing-list">
            <article v-for="order in orders" :key="order.id" class="billing-row">
              <span>
                <strong>{{ order.sku }}</strong>
                <small>{{ formatDate(order.createdAt) }} · ¥{{ money(order.amountMinor) }}</small>
              </span>
              <span class="billing-row-side">
                <b class="num">+{{ order.points }}</b>
                <small>{{ orderStatus(order.status) }}</small>
              </span>
            </article>
            <p v-if="!orders.length" class="empty-state">{{ tx('还没有订单。', 'No orders yet.') }}</p>
          </div>
        </section>

        <section class="panel" aria-labelledby="billing-ledger-title">
          <header class="panel-heading compact">
            <div>
              <h2 id="billing-ledger-title">{{ tx('点数明细', 'Point ledger') }}</h2>
              <p>{{ tx('购买、预留、结算和退款均可追溯。', 'Purchases, reservations, settlements, and refunds remain traceable.') }}</p>
            </div>
          </header>
          <div class="billing-list">
            <article v-for="entry in ledger" :key="entry.id" class="billing-row">
              <span>
                <strong>{{ operationLabel(entry.operationType) }}</strong>
                <small>{{ formatDate(entry.createdAt) }} · {{ entry.referenceType }}</small>
              </span>
              <span class="billing-row-side">
                <b class="num" :class="{ negative: delta(entry) < 0 }">{{ signed(delta(entry)) }}</b>
                <small>{{ tx('余额', 'Balance') }} {{ entry.availableAfter }}</small>
              </span>
            </article>
            <p v-if="!ledger.length" class="empty-state">{{ tx('还没有点数变化。', 'No point activity yet.') }}</p>
          </div>
        </section>
      </div>

      <section class="panel auto-charge-panel" aria-labelledby="auto-charge-title">
        <header class="panel-heading compact">
          <div>
            <h2 id="auto-charge-title">{{ tx('自动整理扣点', 'Automatic organization charging') }}</h2>
            <p>{{ tx('默认关闭；AI 隐私授权不会自动开启扣点。', 'Off by default. AI privacy consent never enables charging automatically.') }}</p>
          </div>
          <label class="billing-switch">
            <input
              v-model="autoPolicy.enabled"
              type="checkbox"
              :aria-label="t('billing.autoChargeAria')"
            />
            <span />
          </label>
        </header>
        <div class="billing-limit-row">
          <label>
            <span>{{ tx('单次上限', 'Per task') }}</span>
            <input v-model.number="autoPolicy.maxPointsPerTask" type="number" min="1" max="100" />
          </label>
          <label>
            <span>{{ tx('每日上限', 'Per day') }}</span>
            <input v-model.number="autoPolicy.maxPointsPerDay" type="number" min="1" max="500" />
          </label>
          <button type="button" class="button secondary" :disabled="savingAuto" @click="saveAutoPolicy">
            <LoaderCircle v-if="savingAuto" class="spin" :size="16" />
            <span>{{ tx('保存设置', 'Save settings') }}</span>
          </button>
        </div>
      </section>

      <section v-if="refunds.length" class="panel" aria-labelledby="billing-refunds-title">
        <header class="panel-heading compact">
          <div>
            <h2 id="billing-refunds-title">{{ tx('退款状态', 'Refund status') }}</h2>
            <p>{{ catalog?.refundPolicy.responseTarget }}</p>
          </div>
        </header>
        <div class="billing-list">
          <article v-for="refund in refunds" :key="refund.id" class="billing-row">
            <span>
              <strong>{{ refundStatus(refund.status) }}</strong>
              <small>{{ formatDate(refund.createdAt) }} · ¥{{ money(refund.refundAmountMinor) }}</small>
              <small>
                {{ tx('未回收', 'Unrecovered') }} {{ refund.unrecoveredPoints }}
                · {{ tx('复核期限', 'Review due') }} {{ formatDate(refund.reviewDueAt) }}
              </small>
              <small v-if="refund.resolution">{{ tx('结论', 'Resolution') }}：{{ refund.resolution }}</small>
            </span>
            <span class="billing-row-side">
              <b class="num">{{ refund.recoveredPoints }}</b>
              <small>{{ tx('已回收点数', 'Points recovered') }}</small>
            </span>
          </article>
        </div>
      </section>

      <p v-if="notice" class="billing-notice" role="status">{{ notice }}</p>
      <p v-if="actionError" class="form-error" role="alert">{{ actionError }}</p>
    </template>
  </section>
</template>

<script setup lang="ts">
import { CircleAlert, CreditCard, LifeBuoy, LoaderCircle } from 'lucide-vue-next'
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import {
  createBillingOrder,
  getAutoChargePolicy,
  getBillingAccount,
  getBillingCatalog,
  getBillingLedger,
  getBillingOrder,
  getBillingOrders,
  getBillingRefunds,
  openBillingCheckout,
  updateAutoChargePolicy,
} from '@/api/client'
import type {
  BillingAccount,
  BillingAutoChargePolicy,
  BillingCatalog,
  BillingLedgerEntry,
  BillingOrder,
  BillingPointPack,
  BillingRefundCase,
} from '@/types/api'

const { t, locale } = useI18n()
const route = useRoute()
const loading = ref(true)
const error = ref('')
const actionError = ref('')
const notice = ref('')
const catalog = ref<BillingCatalog | null>(null)
const account = ref<BillingAccount | null>(null)
const orders = ref<BillingOrder[]>([])
const ledger = ref<BillingLedgerEntry[]>([])
const refunds = ref<BillingRefundCase[]>([])
const activeOrder = ref<BillingOrder | null>(null)
const purchasingSku = ref('')
const savingAuto = ref(false)
const autoPolicy = reactive<BillingAutoChargePolicy>({
  featureScope: 'classify',
  enabled: false,
  maxPointsPerTask: 15,
  maxPointsPerDay: 100,
  timezone: 'Asia/Shanghai',
  consentTextVersion: 'billing-auto-v1',
  version: 1,
  updatedAt: '',
})
let pollTimer: number | undefined

const orderId = computed(() => String(route.query.orderId || ''))

function tx(zh: string, en: string) {
  return locale.value === 'en' ? en : zh
}

function money(value: number) {
  return (value / 100).toFixed(2)
}

function signed(value: number) {
  return value > 0 ? `+${value}` : String(value)
}

function delta(entry: BillingLedgerEntry) {
  if (entry.availableDelta !== 0) return entry.availableDelta
  if (entry.reservedDelta !== 0) return entry.reservedDelta
  return entry.consumedDelta
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat(locale.value === 'en' ? 'en' : 'zh-CN', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}

function orderStatus(status: string) {
  const labels: Record<string, [string, string]> = {
    PENDING: ['待支付', 'Pending payment'],
    CONFIRMING: ['确认中', 'Confirming'],
    PAID: ['已支付', 'Paid'],
    CLOSED: ['已关闭', 'Closed'],
    REFUNDING: ['退款处理中', 'Refund in progress'],
    REFUNDED: ['已退款', 'Refunded'],
    EXCEPTION: ['需复核', 'Needs review'],
  }
  return tx(...(labels[status] ?? [status, status]))
}

function orderTone(status: string) {
  if (status === 'PAID') return 'done'
  if (status === 'EXCEPTION') return 'failed'
  return 'review'
}

function refundStatus(status: string) {
  const labels: Record<string, [string, string]> = {
    noticed: ['已收到退款通知', 'Refund noticed'],
    verifying: ['退款核验中', 'Verifying refund'],
    waiting_task_settlement: ['等待任务结算', 'Waiting for task settlement'],
    recovering: ['正在回收点数', 'Recovering points'],
    recovered: ['已退款并回收', 'Refunded and recovered'],
    partially_recovered: ['部分点数无法回收', 'Partially recovered'],
    rejected: ['退款驳回', 'Refund rejected'],
    manual_review: ['人工复核中', 'Manual review'],
  }
  return tx(...(labels[status] ?? [status, status]))
}

function operationLabel(operation: string) {
  const labels: Record<string, [string, string]> = {
    trial_grant: ['体验点到账', 'Trial points'],
    purchase_grant: ['购买点入账', 'Purchased points'],
    reserve: ['预留点数', 'Points reserved'],
    reserve_release: ['释放预留', 'Reservation released'],
    settle: ['任务结算', 'Task settled'],
    refund_recover: ['退款回收', 'Refund recovery'],
    manual_adjustment: ['人工调整', 'Manual adjustment'],
  }
  return tx(...(labels[operation] ?? [operation, operation]))
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [nextCatalog, nextAccount, nextOrders, nextLedger, nextAuto, nextRefunds] =
      await Promise.all([
        getBillingCatalog(),
        getBillingAccount(),
        getBillingOrders(),
        getBillingLedger(),
        getAutoChargePolicy(),
        getBillingRefunds(),
      ])
    catalog.value = nextCatalog
    account.value = nextAccount
    orders.value = nextOrders.items
    ledger.value = nextLedger.items
    refunds.value = nextRefunds
    Object.assign(autoPolicy, nextAuto)
    if (orderId.value) {
      activeOrder.value = await getBillingOrder(orderId.value)
    } else if (route.name === 'billing-return') {
      activeOrder.value =
        nextOrders.items.find((order) =>
          ['PENDING', 'CONFIRMING'].includes(order.status),
        ) ?? null
    }
    if (activeOrder.value) {
      schedulePoll()
    }
  } catch (caught) {
    error.value = caught instanceof Error ? caught.message : t('errors.generic')
  } finally {
    loading.value = false
  }
}

async function buy(pack: BillingPointPack) {
  if (!catalog.value?.billingEnabled) return
  actionError.value = ''
  notice.value = ''
  purchasingSku.value = pack.sku
  const checkoutWindow =
    typeof window === 'undefined' ? null : window.open('', '_blank')
  if (checkoutWindow) {
    checkoutWindow.opener = null
    checkoutWindow.document.title = tx('正在打开收银台', 'Opening checkout')
    checkoutWindow.document.body.textContent = tx(
      '正在安全打开 Waffo 收银台…',
      'Opening the Waffo checkout securely…',
    )
  }
  try {
    const order = await createBillingOrder(pack.sku)
    const checkout = await openBillingCheckout(order.id)
    activeOrder.value = { ...order, checkoutUrl: checkout.checkoutUrl, status: 'PENDING' }
    if (checkoutWindow) {
      checkoutWindow.location.replace(checkout.checkoutUrl)
      notice.value = tx('收银台已在新标签页打开。', 'Checkout opened in a new tab.')
    } else {
      notice.value = tx(
        '浏览器阻止了新标签页，请使用下方“返回收银台”。',
        'The browser blocked the new tab. Use "Return to checkout" below.',
      )
    }
    schedulePoll()
  } catch (caught) {
    checkoutWindow?.close()
    actionError.value = caught instanceof Error ? caught.message : t('errors.generic')
  } finally {
    purchasingSku.value = ''
  }
}

async function refreshOrder() {
  if (!activeOrder.value) return
  try {
    activeOrder.value = await getBillingOrder(activeOrder.value.id)
    if (activeOrder.value.status === 'PAID') {
      account.value = await getBillingAccount()
      const nextLedger = await getBillingLedger()
      ledger.value = nextLedger.items
    }
  } catch (caught) {
    actionError.value = caught instanceof Error ? caught.message : t('errors.generic')
  }
}

function schedulePoll() {
  window.clearInterval(pollTimer)
  let attempts = 0
  pollTimer = window.setInterval(async () => {
    attempts += 1
    await refreshOrder()
    if (
      attempts >= 10 ||
      !['PENDING', 'CONFIRMING'].includes(activeOrder.value?.status || '')
    ) {
      window.clearInterval(pollTimer)
    }
  }, 3_000)
}

async function saveAutoPolicy() {
  savingAuto.value = true
  actionError.value = ''
  try {
    const updated = await updateAutoChargePolicy({
      enabled: autoPolicy.enabled,
      maxPointsPerTask: autoPolicy.maxPointsPerTask,
      maxPointsPerDay: autoPolicy.maxPointsPerDay,
      consentTextVersion: autoPolicy.consentTextVersion,
      expectedVersion: autoPolicy.version,
    })
    Object.assign(autoPolicy, updated)
    notice.value = tx('自动扣点设置已保存。', 'Automatic charging settings saved.')
  } catch (caught) {
    actionError.value = caught instanceof Error ? caught.message : t('errors.generic')
  } finally {
    savingAuto.value = false
  }
}

onMounted(load)
onBeforeUnmount(() => window.clearInterval(pollTimer))
</script>
