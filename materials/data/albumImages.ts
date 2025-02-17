export interface AlbumImage {
  url: string
  title?: string
  description?: string
}

export interface AlbumData {
  [key: string]: {
    title: string
    description: string
    images: AlbumImage[]
  }
}

export const albumImages: AlbumData = {
  'nature': {
    title: "Nature's Whisper",
    description: 'Collection of natural landscapes',
    images: [
      {
        url: '/albums/nature/1.jpg',
        title: 'Mountain Lake',
        description: 'A serene mountain lake at sunrise'
      },
      {
        url: '/albums/nature/1.jpg',
        title: 'Mountain Lake',
        description: 'A serene mountain lake at sunrise'
      },
      {
        url: '/albums/nature/2.jpg',
        title: 'Mountain Lake',
        description: 'A serene mountain lake at sunrise'
      },
      {
        url: '/albums/nature/3.jpg',
        title: 'Mountain Lake',
        description: 'A serene mountain lake at sunrise'
      },
      {
        url: '/albums/nature/4.jpg',
        title: 'Mountain Lake',
        description: 'A serene mountain lake at sunrise'
      },
      {
        url: '/albums/nature/5.jpg',
        title: 'Mountain Lake',
        description: 'A serene mountain lake at sunrise'
      },
      {
        url: '/albums/nature/6.jpg',
        title: 'Mountain Lake',
        description: 'A serene mountain lake at sunrise'
      },
      {
        url: '/albums/nature/7.jpg',
        title: 'Mountain Lake',
        description: 'A serene mountain lake at sunrise'
      },
      // ... 更多图片
    ]
  },
  'urban': {
    title: 'Urban Stories',
    description: 'City life and architecture',
    images: [
      {
        url: '/albums/urban/1.jpg',
        title: 'City Lights',
        description: 'Night view of the city skyline'
      },
      // ... 更多图片
    ]
  },
  // ... 更多相册
} 