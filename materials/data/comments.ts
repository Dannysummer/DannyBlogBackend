export interface Comment {
  id: number
  floor: number
  nickname: string
  avatar: string
  content: string
  time: string
  likes: number
  image?: string
  replies?: Comment[]
  isLiked?: boolean
}

export const comments: Comment[] = [
    {
      id: 1,
      floor: 1,
      nickname: "小猫咪",
      avatar: "/avatars/avatar1.jpg",
      content: "今天也要开开心心的呀~ ٩(๑❛ᴗ❛๑)۶",
      time: "2024-03-21 12:30",
      likes: 12,
      image: "/background/green-bg.bmp"
    },
    {
      id: 2,
      floor: 2,
      nickname: "奶茶君",
      avatar: "/avatars/avatar2.jpg",
      content: "春天来了，万物复苏，新的一年也要充满希望！🌸",
      time: "2024-03-21 14:15",
      likes: 8,
      image: "/background/gqj.jpg",
      replies: [
        {
          id: 21,
          floor: 2,
          nickname: "小太阳",
          avatar: "/avatars/avatar3.jpg",
          content: "是的呢，春天真好！",
          time: "2024-03-21 14:20",
          likes: 0
        }
      ]
    },
    {
      id: 3,
      floor: 3,
      nickname: "程序猿",
      avatar: "/avatars/avatar4.jpg",
      content: "代码写不完了，但是还是要保持乐观！💪",
      time: "2024-03-21 15:45",
      likes: 15
    },
    {
      id: 4,
      floor: 4,
      nickname: "夜猫子",
      avatar: "/avatars/avatar5.jpg",
      content: "深夜打卡，和大家分享一天的快乐~",
      time: "2024-03-21 23:10",
      likes: 6
    },
    {
      id: 5,
      floor: 5,
      nickname: "画画的小白",
      avatar: "/avatars/avatar5.jpg",
      content: "分享一下今天画的小插画，希望大家喜欢！🎨",
      time: "2024-03-22 10:20",
      likes: 20,
      replies: [
        {
          id: 51,
          floor: 5,
          nickname: "艺术家",
          avatar: "/avatars/avatar4.jpg",
          content: "画得真好看！继续加油！",
          time: "2024-03-22 10:25",
          likes: 0
        }
      ]
    },
    {
      id: 6,
      floor: 6,
      nickname: "旅行者",
      avatar: "/avatars/avatar1.jpg",
      content: "分享一张美丽的风景照，希望带给大家好心情！",
      time: "2024-03-22 11:30",
      likes: 25,
      image: "/background/message-board-bg-dark.jpg"
    },
    {
      id: 7,
      floor: 7,
      nickname: "音乐达人",
      avatar: "/avatars/avatar2.jpg",
      content: "今天推荐一首轻音乐，让心情放松下来~🎵",
      time: "2024-03-22 13:45",
      likes: 18
    },
    {
      id: 8,
      floor: 8,
      nickname: "美食家",
      avatar: "/avatars/avatar3.jpg",
      content: "做了一道新菜，很有成就感！😋",
      time: "2024-03-22 15:20",
      likes: 30,
      image: "/background/purple-bg.bmp"
    },
    {
      id: 9,
      floor: 9,
      nickname: "读书人",
      avatar: "/avatars/avatar5.jpg",
      content: "推荐一本好书：《人间值得》，温暖治愈~📚",
      time: "2024-03-22 16:50",
      likes: 22
    },
    {
      id: 10,
      floor: 10,
      nickname: "摄影师",
      avatar: "/avatars/avatar4.jpg",
      content: "记录生活中的美好瞬间✨",
      time: "2024-03-22 18:15",
      likes: 45,
      image: "/background/sky.jpg"
    },
    {
      id: 11,
      floor: 11,
      nickname: "运动达人",
      avatar: "/avatars/avatar3.jpg",
      content: "坚持运动第100天，和大家分享一下成果！💪",
      time: "2024-03-22 19:40",
      likes: 38
    },
    {
      id: 12,
      floor: 12,
      nickname: "植物爱好者",
      avatar: "/avatars/avatar3.jpg",
      content: "我的小盆栽开花啦！",
      time: "2024-03-22 20:55",
      likes: 27,
      image: "/background/green-bg.bmp"
    },
    {
      id: 13,
      floor: 13,
      nickname: "手工艺人",
      avatar: "/avatars/avatar3.jpg",
      content: "最近在学习编织，虽然还不太熟练，但很有趣！",
      time: "2024-03-22 21:30",
      likes: 19
    },
    {
      id: 14,
      floor: 14,
      nickname: "夜空观察者",
      avatar: "/avatars/avatar4.jpg",
      content: "今晚的星空真美，分享给大家~🌟",
      time: "2024-03-22 22:45",
      likes: 42,
      image: "/background/night.jpg"
    },
    {
      id: 15,
      floor: 15,
      nickname: "咖啡师",
      avatar: "/avatars/avatar5.jpg",
      content: "今天尝试了新的拉花图案，还不错吧？☕",
      time: "2024-03-23 09:15",
      likes: 33,
      image: "/background/mountain-bg.jpg"
    },
    {
      id: 16,
      floor: 16,
      nickname: "宠物达人",
      avatar: "/avatars/avatar3.jpg",
      content: "我家猫咪的日常卖萌时刻～🐱",
      time: "2024-03-23 10:40",
      likes: 55
    },
    {
      id: 17,
      floor: 17,
      nickname: "游戏玩家",
      avatar: "/avatars/avatar1.jpg",
      content: "终于通关了！分享一下游戏截图！🎮",
      time: "2024-03-23 11:55",
      likes: 28,
      image: "/background/gqj.jpg"
    },
    {
      id: 18,
      floor: 18,
      nickname: "园艺爱好者",
      avatar: "/avatars/avatar1.jpg",
      content: "春天到了，花园里的花都开了，美不胜收！🌺",
      time: "2024-03-23 13:20",
      likes: 47,
      image: "/background/green-bg.bmp"
    },
    {
      id: 19,
      floor: 19,
      nickname: "美甲师",
      avatar: "/avatars/avatar4.jpg",
      content: "最新款式分享，喜欢吗？💅",
      time: "2024-03-23 14:45",
      likes: 36
    },
    {
      id: 20,
      floor: 20,
      nickname: "旅行摄影",
      avatar: "/avatars/avatar2.jpg",
      content: "记录旅途中的每一个精彩瞬间✈️",
      time: "2024-03-23 16:10",
      likes: 63,
      image: "/background/sky.jpg"
    }
  ] 