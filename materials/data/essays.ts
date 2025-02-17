export interface Essay {
  id: number
  content: string
  date: Date
  weather: {
    type: 'sunny' | 'cloudy' | 'rainy' | 'snowy' | 'windy'
    temperature: number
  }
  author: string
  avatar: string
  likes: number
  isLiked: boolean
}

export const essaysData: Essay[] = [
  {
    id: 1,
    content: "新的一年开始了，希望能在2024年遇见更好的自己。生活总是充满惊喜，就像今天的阳光一样温暖。",
    date: new Date('2024-01-01T10:15:30'),
    weather: {
      type: 'sunny',
      temperature: 15
    },
    author: 'Danny',
    avatar: '/avatars/default.png',
    likes: 25,
    isLiked: false
  },
  {
    id: 2,
    content: "春节将至，窗外飘着小雪，整个世界都安静了下来。想起小时候守岁的情景，那时的烟花格外绚丽。",
    date: new Date('2024-02-09T23:45:20'),
    weather: {
      type: 'snowy',
      temperature: -2
    },
    author: 'Danny',
    avatar: '/avatars/default.png',
    likes: 18,
    isLiked: false
  },
  {
    id: 3,
    content: "生活就像一盒巧克力，你永远不知道下一颗是什么味道...",
    date: new Date('2024-03-15T15:30:25'),
    weather: {
      type: 'sunny',
      temperature: 23
    },
    author: 'Danny',
    avatar: '/avatars/default.png',
    likes: 15,
    isLiked: false
  },
  {
    id: 4,
    content: "清明时节雨纷纷，写代码的时候特别容易思考人生。今天重构了一下项目，感觉整个世界都清爽了。",
    date: new Date('2024-04-05T09:20:15'),
    weather: {
      type: 'rainy',
      temperature: 18
    },
    author: 'Danny',
    avatar: '/avatars/default.png',
    likes: 12,
    isLiked: false
  },
  {
    id: 5,
    content: "五月的风很温柔，带着些许夏天的气息。今天遇到了一个棘手的bug，但最终解决它时的成就感真好。",
    date: new Date('2024-05-20T14:30:00'),
    weather: {
      type: 'windy',
      temperature: 25
    },
    author: 'Danny',
    avatar: '/avatars/default.png',
    likes: 20,
    isLiked: false
  },
  {
    id: 6,
    content: "盛夏的午后，空调和冰可乐是最好的搭配。想起上学时趴在桌上午睡的日子，那时候的夏天好像没这么热。",
    date: new Date('2024-07-15T13:15:45'),
    weather: {
      type: 'sunny',
      temperature: 32
    },
    author: 'Danny',
    avatar: '/avatars/default.png',
    likes: 16,
    isLiked: false
  },
  {
    id: 7,
    content: "秋高气爽的日子真适合写代码，不知不觉就到了晚上。今天实现了一个很酷的功能，开心！",
    date: new Date('2024-09-28T20:45:30'),
    weather: {
      type: 'cloudy',
      temperature: 22
    },
    author: 'Danny',
    avatar: '/avatars/default.png',
    likes: 22,
    isLiked: false
  },
  {
    id: 8,
    content: "深秋的傍晚特别容易感怀，看着项目一步步成长，就像看着自己的孩子长大一样。",
    date: new Date('2024-10-31T17:50:10'),
    weather: {
      type: 'windy',
      temperature: 16
    },
    author: 'Danny',
    avatar: '/avatars/default.png',
    likes: 19,
    isLiked: false
  },
  {
    id: 9,
    content: "冬天的第一场雪总是让人惊喜，窗外的世界银装素裹。趁着这么美的雪景，给项目加了些雪花特效。",
    date: new Date('2024-12-20T11:25:40'),
    weather: {
      type: 'snowy',
      temperature: -1
    },
    author: 'Danny',
    avatar: '/avatars/default.png',
    likes: 24,
    isLiked: false
  },
  {
    id: 10,
    content: "2025年的第一天，回首过去一年，经历了很多，成长了很多。新的一年，继续努力！",
    date: new Date('2025-01-01T00:00:01'),
    weather: {
      type: 'cloudy',
      temperature: 5
    },
    author: 'Danny',
    avatar: '/avatars/default.png',
    likes: 30,
    isLiked: false
  }
] 