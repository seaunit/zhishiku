import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUiStore = defineStore('ui', () => {
  const menuOpen = ref(false)

  function closeMenu() {
    menuOpen.value = false
  }

  function toggleMenu() {
    menuOpen.value = !menuOpen.value
  }

  return { menuOpen, closeMenu, toggleMenu }
})

