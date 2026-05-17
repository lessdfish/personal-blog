import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

type Wallpaper = {
  name: string
  image: string
  position: string
  overlay: string
}

const wallpapers: Wallpaper[] = [
  {
    name: 'Sunrise',
    image: '/forum-fantasy-girl.jpg',
    position: 'center top',
    overlay: 'linear-gradient(180deg, rgba(6, 8, 14, 0.28), rgba(8, 10, 16, 0.52))',
  },
  {
    name: 'Focus',
    image: '/forum-fantasy-girl.jpg',
    position: '58% 22%',
    overlay: 'linear-gradient(180deg, rgba(7, 12, 16, 0.44), rgba(10, 13, 18, 0.66))',
  },
  {
    name: 'Warm',
    image: '/forum-fantasy-girl.jpg',
    position: 'center 35%',
    overlay: 'linear-gradient(180deg, rgba(12, 8, 8, 0.36), rgba(11, 12, 18, 0.62))',
  },
]

const STORAGE_KEY = 'blog-wallpaper-index'

/**
 * 提供壁纸轮换能力：按时间间隔切换页面背景图。
 */
export function useWallpaperRotation(intervalMs = 90000) {
  const currentIndex = ref(0)
  let timer: number | undefined

  const currentWallpaper = computed(() => wallpapers[currentIndex.value] || wallpapers[0])

  /**
   * 应用当前壁纸：把选中的图片写入页面 CSS 变量。
   */
  function applyWallpaper() {
    const wallpaper = currentWallpaper.value
    document.documentElement.style.setProperty('--wallpaper-image', `url('${wallpaper.image}')`)
    document.documentElement.style.setProperty('--wallpaper-position', wallpaper.position)
    document.documentElement.style.setProperty('--wallpaper-overlay', wallpaper.overlay)
  }

  /**
   * 轮换下一张壁纸：更新索引后重新应用背景图。
   */
  function rotateWallpaper() {
    currentIndex.value = (currentIndex.value + 1) % wallpapers.length
    localStorage.setItem(STORAGE_KEY, String(currentIndex.value))
    applyWallpaper()
  }

  onMounted(() => {
    const saved = Number(localStorage.getItem(STORAGE_KEY))
    currentIndex.value = Number.isInteger(saved) && saved >= 0 ? saved % wallpapers.length : 0
    applyWallpaper()
    timer = window.setInterval(rotateWallpaper, intervalMs)
  })

  onBeforeUnmount(() => {
    if (timer) {
      window.clearInterval(timer)
    }
  })

  return {
    currentWallpaper,
    rotateWallpaper,
  }
}
