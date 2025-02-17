import markdownTest from './articleMd/markdown语法测试集合.md?raw'
import cssComputeProcess from './articleMd/CSS 属性计算过程.md?raw'

export interface Article {
  id: number
  title: string
  createTime: string
  updateTime: string
  views: number
  cover: string
  content: string
  description?: string
  aiSummary?: string
  tags?: string[]
  category?: string
  author?: string
  license?: string
}

export interface TimelineYear {
  year: string
  articles: Article[]
}

export const timelineData: TimelineYear[] = [
  {
    year: '2025',
    articles: [
      {
        id: 1,
        title: 'Cloudflare/Vercel项目推荐(3)',
        createTime: '2025-01-14',
        updateTime: '2025-01-14',
        views: 558,
        cover: '/articles/cover/3.jpg',
        content: `# Cloudflare/Vercel项目推荐

## 前言
这是一篇关于云服务的详细文章。

## 主要内容
1. Cloudflare Workers
2. Vercel Serverless Functions
3. Edge Computing

## 技术细节
\`\`\`javascript
const worker = new Worker();
console.log("Hello from Worker!");
\`\`\`

> 这是一个重要的引用

更多内容请继续阅读...`,
        description: '推荐一些基于 Cloudflare/Vercel 的优质开源项目',
        tags: ['Cloudflare', 'Vercel', '项目推荐', '开源'],
        category: '技术分享',
        aiSummary: '这篇文章详细介绍了 Cloudflare/Vercel 项目的推荐，包括 Cloudflare Workers、Vercel Serverless Functions 和 Edge Computing 的技术细节。',
        author: 'LiuShen',
        license: 'CC BY-NC-SA 4.0'
      },
      {
        id: 2,
        title: 'CSS属性计算过程详解',
        createTime: '2025-05-14',
        updateTime: '2025-05-14',
        views: 558,
        cover: '/articles/cover/3.jpg',
        content: cssComputeProcess,
        description: 'CSS属性计算过程的详细解析',
        tags: ['CSS', '前端', '技术文章'],
        category: '技术分享',
        aiSummary: '这篇文章详细介绍了 CSS 属性的计算过程，包括声明值确定、层叠冲突解决、继承和默认值的使用等内容。通过实例讲解了不同来源样式的优先级以及选择器权重的计算方式。',
        author: 'Duyi',
        license: 'CC BY-NC-SA 4.0'
      },
      {
        id: 12,
        title: 'Markdown语法测试集合',
        createTime: '2025-01-14',
        updateTime: '2025-01-14',
        views: 558,
        cover: '/articles/cover/3.jpg',
        content: markdownTest,
        description: 'Markdown语法的完整测试文档',
        tags: ['Markdown', '测试', '文档'],
        category: '技术分享',
        aiSummary: '这篇文章详细介绍了 Markdown 语法的完整测试文档，包括各种测试用例和实际应用场景。',
        author: 'LiuShen',
        license: 'CC BY-NC-SA 4.0'
      }
    ]
  },
  {
    year: '2024',
    articles: [
      {
        id: 12,
        title: 'MD样式测试文本',
        createTime: '2024-12-31',
        updateTime: '2025-01-14',
        views: 326,
        cover: '/articles/cover/1.jpg',
        content: ``,
        description: '2024年终总结',
        tags: ['年终总结', '生活感悟', '2024'],
        category: '日常分享',
        aiSummary: '这篇文章总结了2024年的生活和工作经历，分享了作者在这一年中的感悟和成长。',
        author: 'LiuShen',
        license: 'CC BY-NC-SA 4.0'
      },
      {
        id: 3,
        title: '华为通用软件开发工程师面经',
        createTime: '2024-12-25',
        updateTime: '2025-01-14',
        views: 445,
        cover: '/articles/cover/2.jpg',
        content: '# Cloudflare/Vercel项目推荐\n\n这是一篇关于云服务的文章...',
        description: '华为通用软件开发工程师面试经验分享',
        tags: ['面试', '华为', '经验分享', '求职'],
        category: '学习资料',
        aiSummary: '这篇文章分享了华为通用软件开发工程师面试的经验，包括面试流程、准备技巧和面试官可能会问到的问题。',
        author: 'LiuShen',
        license: 'CC BY-NC-SA 4.0'
      }
    ]
  },
  {
    year: '2023',
    articles: [
      {
        id: 2,
        title: '2024，清风入梦，扬帆待明年',
        createTime: '2024-12-31',
        updateTime: '2025-01-14',
        views: 326,
        cover: '/articles/cover/1.jpg',
        content: '# Cloudflare/Vercel项目推荐\n\n这是一篇关于云服务的文章...',
        description: '2024年终总结',
        category: '年度总结',
        aiSummary: '这篇文章总结了2024年的工作和生活经历，展望了未来的计划和目标。',
        author: 'LiuShen',
        license: 'CC BY-NC-SA 4.0'
      },
      {
        id: 3,
        title: '华为通用软件开发工程师面经',
        createTime: '2024-12-25',
        updateTime: '2025-01-14',
        views: 445,
        cover: '/articles/cover/2.jpg',
        content: '# Cloudflare/Vercel项目推荐\n\n这是一篇关于云服务的文章...',
        description: '华为通用软件开发工程师面试经验分享',
        aiSummary: '这篇文章分享了华为通用软件开发工程师面试的经验，包括面试流程、准备技巧和面试官可能会问到的问题。',
        author: 'LiuShen',
        license: 'CC BY-NC-SA 4.0'
      },
      {
        id: 4,
        title: '华为通用软件开发工程师面经',
        createTime: '2024-12-25',
        updateTime: '2025-01-14',
        views: 445,
        cover: '/articles/cover/2.jpg',
        content: '# Cloudflare/Vercel项目推荐\n\n这是一篇关于云服务的文章...',
        description: '华为通用软件开发工程师面试经验分享',
        aiSummary: '这篇文章分享了华为通用软件开发工程师面试的经验，包括面试流程、准备技巧和面试官可能会问到的问题。',
        author: 'LiuShen',
        license: 'CC BY-NC-SA 4.0'
      }
    ]
  },
  {
    year: '2022',
    articles: [
      {
        id: 2,
        title: '2024，清风入梦，扬帆待明年',
        createTime: '2022-12-31',
        updateTime: '2025-01-14',
        views: 326,
        cover: '/articles/cover/1.jpg',
        content: '# Cloudflare/Vercel项目推荐\n\n这是一篇关于云服务的文章...1223132',
        description: '2024年终总结',
        aiSummary: '这篇文章总结了2024年的工作和生活经历，展望了未来的计划和目标。',
        author: 'LiuShen',
        license: 'CC BY-NC-SA 4.0'
      },
      {
        id: 3,
        title: 'test',
        createTime: '2022-12-25',
        updateTime: '2025-01-14',
        views: 445,
        cover: '/articles/cover/2.jpg',
        content: `Lorem ipsum dolor sit, amet consectetur adipisicing elit. Modi, hic.
        Placeat ut commodi id dolores iure ipsa fugiat esse. Autem?
        Minima nihil ex voluptatum officia architecto fuga vitae sit earum?
        Cum harum animi dolorum molestias adipisci iure praesentium doloribus tempore?
        Officia neque porro fugit velit ex similique? Delectus, tempora dolorem.
        Alias deleniti nesciunt similique consequatur porro odio tempora quod. Repudiandae.
        Soluta hic omnis quos debitis a iure obcaecati molestias cupiditate.
        Deserunt voluptatibus voluptatem velit officia nam? Omnis aliquam culpa provident!
        Pariatur aperiam aliquid fugit recusandae odit officia quidem culpa velit.
        Adipisci ad amet nisi consequatur excepturi nostrum sed voluptatibus quas!
        Cum, quia. Eveniet nisi in fuga expedita eius iure tempore?
        Nobis odit error numquam consectetur voluptate amet repudiandae. Expedita, alias!
        Voluptatum numquam, cum tempore ullam minus quidem error natus aliquam.
        Et consequuntur maiores eaque optio, officia quam dolor molestiae suscipit!
        Nam ut maiores officiis laborum sunt recusandae exercitationem repellat! Delectus.
        Possimus neque asperiores ut eveniet quaerat nam veniam error quas.
        Illo, deserunt rerum ipsam temporibus blanditiis facilis fugiat repudiandae nam!
        Ad nemo dolore magnam necessitatibus odio perspiciatis earum eum a.
        Doloremque aut tenetur minima dolor exercitationem? Qui doloremque aperiam quos!
        Esse expedita id nam veritatis fuga voluptatem sit, numquam inventore.
        Quo obcaecati omnis tenetur eveniet excepturi mollitia, laudantium voluptates blanditiis.
        Excepturi autem quas similique tempore vitae quidem cum quod libero.
        Aperiam molestiae quod, ducimus aspernatur saepe nulla porro? Nobis, dolorum.
        Assumenda totam quasi illum. Voluptatibus recusandae non accusamus maxime quaerat.
        Doloremque quasi distinctio deleniti ipsa nesciunt quo beatae voluptas quis.`,
        description: '华为通用软件开发工程师面试经验分享',
        aiSummary: '这篇文章分享了华为通用软件开发工程师面试的经验，包括面试流程、准备技巧和面试官可能会问到的问题。',
        author: 'LiuShen',
        license: 'CC BY-NC-SA 4.0'
      },
      {
        id: 4,
        title: '华为通用软件开发工程师面经',
        createTime: '2022-12-25',
        updateTime: '2025-01-14',
        views: 445,
        cover: '/articles/cover/2.jpg',
        content: '# Cloudflare/Vercel项目推荐\n\n这是一篇关于云服务的文章...',
        description: '华为通用软件开发工程师面试经验分享',
        aiSummary: '这篇文章分享了华为通用软件开发工程师面试的经验，包括面试流程、准备技巧和面试官可能会问到的问题。',
        author: 'LiuShen',
        license: 'CC BY-NC-SA 4.0'
      }
    ]
  },
  {
    year: '2021',
    articles: [
      {
        id: 2,
        title: '2024，清风入梦，扬帆待明年',
        createTime: '2021-07-31',
        updateTime: '2025-01-14',
        views: 326,
        cover: '/articles/cover/1.jpg',
        content: '# Cloudflare/Vercel项目推荐\n\n这是一篇关于云服务的文章...',
        description: '2024年终总结',
        aiSummary: '这篇文章总结了2024年的工作和生活经历，展望了未来的计划和目标。',
        author: 'LiuShen',
        license: 'CC BY-NC-SA 4.0'
      },
      {
        id: 3,
        title: '华为通用软件开发工程师面经',
        createTime: '2021-12-25',
        updateTime: '2025-01-14',
        views: 445,
        cover: '/articles/cover/2.jpg',
        content: '# Cloudflare/Vercel项目推荐\n\n这是一篇关于云服务的文章...',
        description: '华为通用软件开发工程师面试经验分享',
        aiSummary: '这篇文章分享了华为通用软件开发工程师面试的经验，包括面试流程、准备技巧和面试官可能会问到的问题。',
        author: 'LiuShen',
        license: 'CC BY-NC-SA 4.0'
      },
      {
        id: 4,
        title: '华为通用软件开发工程师面经',
        createTime: '2021-05-25',
        updateTime: '2025-01-14',
        views: 445,
        cover: '/articles/cover/2.jpg',
        content: '# Cloudflare/Vercel项目推荐\n\n这是一篇关于云服务的文章...',
        description: '华为通用软件开发工程师面试经验分享',
        aiSummary: '这篇文章分享了华为通用软件开发工程师面试的经验，包括面试流程、准备技巧和面试官可能会问到的问题。',
        author: 'LiuShen',
        license: 'CC BY-NC-SA 4.0'
      }
    ]
  }
]

export const getArticleById = (id: number): Article | undefined => {
  for (const year of timelineData) {
    const article = year.articles.find(article => article.id === id)
    if (article) return article
  }
  return undefined
} 