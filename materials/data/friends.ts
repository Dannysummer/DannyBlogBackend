// 定义友链类型
export interface FriendLink {
  avatar: string
  cover: string
  name: string
  description: string
  url: string
  delay?: string
  category: 'bigshot' | 'close' | 'friend' | 'tech'
}

// 友链数据
export const friends: FriendLink[] = [
  // 大佬分类
  {
    avatar: '/friends/bigshot/avatars/avatar1.jpg',
    cover: '/friends/bigshot/covers/cover1.jpg',
    name: '熊小咔',
    description: '分享技术与生活的点滴记录，分享技术与生活的点滴记录',
    url: 'https://blog.liushen.fun',
    delay: '10ms',
    category: 'bigshot'
  },
  {
    avatar: '/friends/bigshot/avatars/avatar2.jpg',
    cover: '/friends/bigshot/covers/cover2.jpg',
    name: 'TechMaster',
    description: '全栈开发者，热爱分享技术见解',
    url: 'https://example.com',
    delay: '20ms',
    category: 'bigshot'
  },
  {
    avatar: '/friends/bigshot/avatars/avatar3.jpg',
    cover: '/friends/bigshot/covers/cover3.jpg',
    name: 'CodeWizard',
    description: '用代码书写人生，用技术改变世界',
    url: 'https://example.com',
    delay: '30ms',
    category: 'bigshot'
  },
  {
    avatar: '/friends/bigshot/avatars/avatar4.jpg',
    cover: '/friends/bigshot/covers/cover4.jpg',
    name: '极客漫步',
    description: '探索编程的无限可能',
    url: 'https://example.com',
    delay: '40ms',
    category: 'bigshot'
  },
  {
    avatar: '/friends/bigshot/avatars/avatar5.jpg',
    cover: '/friends/bigshot/covers/cover5.jpg',
    name: '云上漫步',
    description: '云原生技术专家，架构师',
    url: 'https://example.com',
    delay: '50ms',
    category: 'bigshot'
  },

  // 密友分类
  {
    avatar: '/friends/close/avatars/avatar1.jpg',
    cover: '/friends/close/covers/cover1.jpg',
    name: '青柠',
    description: '一起成长的挚友',
    url: 'https://example.com',
    delay: '60ms',
    category: 'close'
  },
  {
    avatar: '/friends/close/avatars/avatar2.jpg',
    cover: '/friends/close/covers/cover2.jpg',
    name: '木子李',
    description: '分享生活，记录点滴',
    url: 'https://example.com',
    delay: '70ms',
    category: 'close'
  },
  {
    avatar: '/friends/close/avatars/avatar3.jpg',
    cover: '/friends/close/covers/cover3.jpg',
    name: '南风知我',
    description: '一起追逐梦想的伙伴',
    url: 'https://example.com',
    delay: '80ms',
    category: 'close'
  },
  {
    avatar: '/friends/close/avatars/avatar4.jpg',
    cover: '/friends/close/covers/cover4.jpg',
    name: '晨曦',
    description: '永远积极向上的好友',
    url: 'https://example.com',
    delay: '90ms',
    category: 'close'
  },
  {
    avatar: '/friends/close/avatars/avatar5.jpg',
    cover: '/friends/close/covers/cover5.jpg',
    name: '初见',
    description: '记录生活美好时光',
    url: 'https://example.com',
    delay: '100ms',
    category: 'close'
  },

  // 普通朋友分类
  {
    avatar: '/friends/friend/avatars/avatar1.jpg',
    cover: '/friends/friend/covers/cover1.jpg',
    name: '流年',
    description: '记录生活，享受当下',
    url: 'https://example.com',
    delay: '110ms',
    category: 'friend'
  },
  {
    avatar: '/friends/friend/avatars/avatar2.jpg',
    cover: '/friends/friend/covers/cover2.jpg',
    name: '远方',
    description: '分享旅行与摄影',
    url: 'https://example.com',
    delay: '120ms',
    category: 'friend'
  },
  {
    avatar: '/friends/friend/avatars/avatar3.jpg',
    cover: '/friends/friend/covers/cover3.jpg',
    name: '青衫',
    description: '热爱生活的博主',
    url: 'https://example.com',
    delay: '130ms',
    category: 'friend'
  },
  {
    avatar: '/friends/friend/avatars/avatar4.jpg',
    cover: '/friends/friend/covers/cover4.jpg',
    name: '墨痕',
    description: '记录编程学习历程',
    url: 'https://example.com',
    delay: '140ms',
    category: 'friend'
  },
  {
    avatar: '/friends/friend/avatars/avatar5.jpg',
    cover: '/friends/friend/covers/cover5.jpg',
    name: '静待花开',
    description: '分享技术与生活感悟',
    url: 'https://example.com',
    delay: '150ms',
    category: 'friend'
  },

  // 官方技术博客分类
  {
    avatar: '/friends/tech/avatars/avatar1.jpg',
    cover: '/friends/tech/covers/cover1.jpg',
    name: 'Vue Blog',
    description: 'Vue.js 官方博客',
    url: 'https://blog.vuejs.org',
    delay: '160ms',
    category: 'tech'
  },
  {
    avatar: '/friends/tech/avatars/avatar2.jpg',
    cover: '/friends/tech/covers/cover2.jpg',
    name: 'React Blog',
    description: 'React 官方博客',
    url: 'https://reactjs.org/blog',
    delay: '170ms',
    category: 'tech'
  },
  {
    avatar: '/friends/tech/avatars/avatar3.jpg',
    cover: '/friends/tech/covers/cover3.jpg',
    name: 'TypeScript',
    description: 'TypeScript 官方博客',
    url: 'https://devblogs.microsoft.com/typescript',
    delay: '180ms',
    category: 'tech'
  },
  {
    avatar: '/friends/tech/avatars/avatar4.jpg',
    cover: '/friends/tech/covers/cover4.jpg',
    name: 'Node.js Blog',
    description: 'Node.js 官方博客',
    url: 'https://nodejs.org/en/blog',
    delay: '190ms',
    category: 'tech'
  },
  {
    avatar: '/friends/tech/avatars/avatar5.jpg',
    cover: '/friends/tech/covers/cover5.jpg',
    name: 'MDN Web Docs',
    description: 'Mozilla 开发者网络',
    url: 'https://developer.mozilla.org/blog',
    delay: '200ms',
    category: 'tech'
  }
]

// 分类名称映射
export const categoryNames = {
  all: '全部',
  bigshot: '大佬',
  close: '密友',
  friend: '普通朋友',
  tech: '官方技术博客'
} 