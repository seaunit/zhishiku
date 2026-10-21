import { defineStore } from 'pinia'
import { reactive } from 'vue'

export const useMediaStore = defineStore('media', () => {
  const previews = reactive<Record<string, string>>({})

  function setPreview(assetId: string, file: File) {
    const existing = previews[assetId]
    if (existing) {
      URL.revokeObjectURL(existing)
    }
    previews[assetId] = URL.createObjectURL(file)
  }

  function getPreview(assetId: string): string | undefined {
    return previews[assetId]
  }

  function clearPreviews() {
    Object.values(previews).forEach((url) => URL.revokeObjectURL(url))
    Object.keys(previews).forEach((key) => {
      delete previews[key]
    })
  }

  return { previews, setPreview, getPreview, clearPreviews }
})

