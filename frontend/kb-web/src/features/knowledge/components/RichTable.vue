<template>
  <div
    class="rich-table-frame"
    role="region"
    :aria-label="t('detail.tableLabel')"
    tabindex="0"
  >
    <table class="rich-table-view">
      <thead>
        <tr>
          <th
            v-for="(cell, index) in headerCells"
            :key="index"
            scope="col"
            :colspan="Number(cell.attrs?.colspan || 1)"
            :rowspan="Number(cell.attrs?.rowspan || 1)"
          >
            <CellText :node="cell" />
          </th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(row, rowIndex) in bodyRows" :key="rowIndex">
          <td
            v-for="(cell, cellIndex) in row.content || []"
            :key="cellIndex"
            :colspan="Number(cell.attrs?.colspan || 1)"
            :rowspan="Number(cell.attrs?.rowspan || 1)"
          >
            <CellText :node="cell" />
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
import { computed, defineComponent, h } from 'vue'
import { useI18n } from 'vue-i18n'
import InlineNode from '@/components/InlineNode.vue'
import type { RichTextNode } from '@/types/rich-text'

const props = defineProps<{
  node: RichTextNode
}>()

const { t } = useI18n()

const headerCells = computed(() => props.node.content?.[0]?.content || [])
const bodyRows = computed(() => props.node.content?.slice(1) || [])

const CellText = defineComponent({
  props: {
    node: {
      type: Object as () => RichTextNode,
      required: true,
    },
  },
  setup(cellProps) {
    return () =>
      (cellProps.node.content || []).map((block, blockIndex) =>
        h(
          'p',
          { key: blockIndex },
          block.content?.map((child, childIndex) =>
            h(InlineNode, { node: child, key: childIndex }),
          ),
        ),
      )
  },
})
</script>
