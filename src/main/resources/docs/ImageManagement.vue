<template>
  <div class="image-management-page">
    <div class="page-header">
      <h1>图床管理</h1>
      <div class="header-actions">
        <button class="refresh-btn" @click="fetchImages">
          <Icon icon="mdi:refresh" :class="{ 'rotating': isLoading }" />
          刷新
        </button>
        <button class="upload-btn" @click="showUploadModal = true">
          <Icon icon="mdi:cloud-upload" />
          上传图片
        </button>
      </div>
    </div>

    <!-- 筛选选项 -->
    <div class="filter-options">
      <div class="filter-group">
        <label>类型筛选：</label>
        <select v-model="filterType">
          <option value="all">全部类型</option>
          <option value="image">图片</option>
          <option value="avatar">头像</option>
          <option value="cover">封面</option>
          <option value="article">文章图片</option>
        </select>
      </div>
      
      <div class="filter-group">
        <label>时间排序：</label>
        <select v-model="sortOrder">
          <option value="newest">最新上传</option>
          <option value="oldest">最早上传</option>
        </select>
      </div>
      
      <div class="search-group">
        <input type="text" v-model="searchQuery" placeholder="搜索文件名..." @keydown.enter="fetchImages" />
        <button @click="fetchImages">
          <Icon icon="mdi:magnify" />
        </button>
      </div>
    </div>

    <!-- 图片列表 -->
    <div class="images-container">
      <div v-if="images.length > 0" class="images-grid">
        <div v-for="image in images" :key="image.id" class="image-card">
          <div class="image-preview">
            <img :src="image.url" :alt="image.name" @click="previewImage(image)" />
          </div>
          <div class="image-info">
            <div class="image-name" :title="image.name">{{ truncateText(image.name, 20) }}</div>
            <div class="image-details">
              <span class="image-size">{{ formatSize(image.size) }}</span>
              <span class="image-date">{{ formatDate(image.createdAt) }}</span>
            </div>
          </div>
          <div class="image-actions">
            <button class="copy-btn" @click="copyImageUrl(image.url)">
              <Icon icon="mdi:content-copy" />
            </button>
            <button class="delete-btn" @click="confirmDeleteImage(image)">
              <Icon icon="mdi:delete" />
            </button>
          </div>
        </div>
      </div>
      
      <!-- 没有图片时显示 -->
      <div v-if="images.length === 0 && !isLoading" class="no-images">
        <Icon icon="mdi:image-off" class="no-images-icon" />
        <p>暂无图片数据</p>
      </div>
      
      <!-- 加载中 -->
      <div v-if="isLoading" class="loading-container">
        <div class="loading-spinner"></div>
        <p>加载中...</p>
      </div>
    </div>

    <!-- 上传图片对话框 -->
    <div class="modal" v-if="showUploadModal" @click.self="showUploadModal = false">
      <div class="modal-content">
        <h3>上传图片</h3>
        <div class="modal-body">
          <div class="form-group">
            <label for="image-type">图片类型</label>
            <select id="image-type" v-model="uploadForm.type">
              <option value="image">普通图片</option>
              <option value="avatar">头像</option>
              <option value="cover">封面</option>
              <option value="article">文章图片</option>
            </select>
          </div>
          
          <div class="upload-area" 
            @dragover.prevent="dragover" 
            @dragleave.prevent="dragleave" 
            @drop.prevent="handleDrop"
            :class="{ 'active': isDragging || uploadForm.files.length > 0 }"
          >
            <input 
              type="file" 
              ref="fileInput" 
              multiple 
              accept="image/*" 
              @change="handleFileSelect" 
              style="display: none"
            />
            
            <div v-if="uploadForm.files.length === 0" class="upload-placeholder">
              <Icon icon="mdi:cloud-upload" class="upload-icon" />
              <p>拖放图片到此处，或 <span class="select-files" @click="selectFiles">选择文件</span></p>
            </div>
            
            <div v-else class="selected-files">
              <div v-for="(file, index) in uploadForm.files" :key="index" class="selected-file">
                <div class="selected-file-preview">
                  <img :src="getPreviewUrl(file)" :alt="file.name" />
                </div>
                <div class="selected-file-info">
                  <div class="selected-file-name">{{ truncateText(file.name, 20) }}</div>
                  <div class="selected-file-size">{{ formatSize(file.size) }}</div>
                </div>
                <button class="remove-file-btn" @click="removeFile(index)">
                  <Icon icon="mdi:close" />
                </button>
              </div>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="cancel-btn" @click="showUploadModal = false">取消</button>
          <button 
            class="confirm-btn" 
            @click="uploadImages"
            :disabled="uploadForm.files.length === 0 || isUploading"
          >
            <span v-if="isUploading">上传中...</span>
            <span v-else>上传</span>
          </button>
        </div>
      </div>
    </div>

    <!-- 图片预览对话框 -->
    <div class="modal" v-if="showPreviewModal" @click.self="showPreviewModal = false">
      <div class="preview-modal-content">
        <div class="preview-image-container">
          <img :src="previewImageUrl" alt="预览图片" />
        </div>
        <div class="preview-info">
          <div class="preview-name">{{ currentImage?.name }}</div>
          <div class="preview-details">
            <div class="preview-detail">
              <Icon icon="mdi:calendar" />
              <span>{{ formatDate(currentImage?.createdAt) }}</span>
            </div>
            <div class="preview-detail">
              <Icon icon="mdi:file-size" />
              <span>{{ formatSize(currentImage?.size) }}</span>
            </div>
            <div class="preview-detail">
              <Icon icon="mdi:file-type" />
              <span>{{ currentImage?.type }}</span>
            </div>
          </div>
          <div class="preview-actions">
            <button class="preview-btn" @click="copyImageUrl(currentImage?.url)">
              <Icon icon="mdi:content-copy" />
              复制链接
            </button>
            <button class="preview-btn" @click="downloadImage">
              <Icon icon="mdi:download" />
              下载
            </button>
            <button class="preview-delete-btn" @click="confirmDeleteImage(currentImage)">
              <Icon icon="mdi:delete" />
              删除
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 删除确认对话框 -->
    <div class="modal" v-if="showDeleteConfirm" @click.self="showDeleteConfirm = false">
      <div class="modal-content">
        <h3>删除图片</h3>
        <div class="modal-body">
          <p>确定要删除图片 <strong>{{ currentImage?.name }}</strong> 吗？此操作不可撤销。</p>
        </div>
        <div class="modal-footer">
          <button class="cancel-btn" @click="showDeleteConfirm = false">取消</button>
          <button class="delete-confirm-btn" @click="deleteImage">确认删除</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { Icon } from '@iconify/vue'

interface Image {
  id: number
  name: string
  url: string
  type: string
  size: number
  createdAt: string
  updatedAt: string
}

const images = ref<Image[]>([])
const isLoading = ref(false)
const isUploading = ref(false)
const showUploadModal = ref(false)
const showPreviewModal = ref(false)
const showDeleteConfirm = ref(false)
const currentImage = ref<Image | null>(null)
const isDragging = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)
const previewImageUrl = ref('')

// 筛选和排序
const filterType = ref('all')
const sortOrder = ref('newest')
const searchQuery = ref('')

// 上传表单
const uploadForm = ref({
  type: 'image',
  files: [] as File[]
})

// 获取图片数据
const fetchImages = async () => {
  isLoading.value = true
  try {
    // 构建查询参数
    const params = new URLSearchParams()
    if (filterType.value !== 'all') {
      params.append('type', filterType.value)
    }
    params.append('sort', sortOrder.value)
    if (searchQuery.value) {
      params.append('search', searchQuery.value)
    }
    
    const response = await fetch(`http://localhost:8088/api/images?${params.toString()}`, {
      credentials: 'include'
    })
    const data = await response.json()
    
    if (data.success) {
      images.value = data.data || []
      console.log('图片列表:', images.value)
    } else {
      throw new Error(data.message || '获取图片失败')
    }
  } catch (error: any) {
    console.error('获取图片失败:', error)
    showAlert(error.message || '获取图片失败，请重试', 'error')
  } finally {
    isLoading.value = false
  }
}

// 预览图片
const previewImage = (image: Image) => {
  currentImage.value = image
  previewImageUrl.value = image.url
  showPreviewModal.value = true
}

// 格式化日期
const formatDate = (dateString?: string) => {
  if (!dateString) return '-'
  const date = new Date(dateString)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

// 格式化文件大小
const formatSize = (bytes?: number) => {
  if (bytes === undefined) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
}

// 截断文本
const truncateText = (text: string, maxLength: number) => {
  if (text.length <= maxLength) return text
  return text.slice(0, maxLength) + '...'
}

// 选择文件
const selectFiles = () => {
  fileInput.value?.click()
}

// 处理文件选择
const handleFileSelect = (event: Event) => {
  const input = event.target as HTMLInputElement
  if (input.files && input.files.length > 0) {
    uploadForm.value.files = [...uploadForm.value.files, ...Array.from(input.files)]
    input.value = '' // 清空input，允许再次选择相同文件
  }
}

// 处理拖放
const dragover = (event: DragEvent) => {
  isDragging.value = true
}

const dragleave = (event: DragEvent) => {
  isDragging.value = false
}

const handleDrop = (event: DragEvent) => {
  isDragging.value = false
  if (event.dataTransfer?.files) {
    const files = Array.from(event.dataTransfer.files).filter(file => file.type.startsWith('image/'))
    uploadForm.value.files = [...uploadForm.value.files, ...files]
  }
}

// 获取预览URL
const getPreviewUrl = (file: File) => {
  return URL.createObjectURL(file)
}

// 移除文件
const removeFile = (index: number) => {
  uploadForm.value.files.splice(index, 1)
}

// 复制图片URL
const copyImageUrl = (url?: string) => {
  if (!url) return
  
  navigator.clipboard.writeText(url)
    .then(() => {
      showAlert('已复制图片链接到剪贴板', 'success')
    })
    .catch(err => {
      console.error('复制失败:', err)
      showAlert('复制失败，请手动复制', 'error')
    })
}

// 下载图片
const downloadImage = async () => {
  if (!currentImage.value) return
  
  try {
    const response = await fetch(currentImage.value.url)
    const blob = await response.blob()
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = currentImage.value.name
    document.body.appendChild(a)
    a.click()
    window.URL.revokeObjectURL(url)
    document.body.removeChild(a)
  } catch (error: any) {
    console.error('下载失败:', error)
    showAlert('下载失败，请重试', 'error')
  }
}

// 确认删除图片
const confirmDeleteImage = (image: Image | null) => {
  if (!image) return
  
  currentImage.value = image
  showDeleteConfirm.value = true
  showPreviewModal.value = false
}

// 删除图片
const deleteImage = async () => {
  if (!currentImage.value) return
  
  try {
    const response = await fetch(`http://localhost:8088/api/images/${currentImage.value.id}`, {
      method: 'DELETE',
      credentials: 'include'
    })
    
    const data = await response.json()
    if (data.success) {
      showAlert('删除图片成功', 'success')
      showDeleteConfirm.value = false
      currentImage.value = null
      await fetchImages()
    } else {
      throw new Error(data.message || '删除图片失败')
    }
  } catch (error: any) {
    console.error('删除图片失败:', error)
    showAlert(error.message || '删除图片失败，请重试', 'error')
  }
}

// 上传图片
const uploadImages = async () => {
  if (uploadForm.value.files.length === 0) return
  
  isUploading.value = true
  try {
    const formData = new FormData()
    uploadForm.value.files.forEach(file => {
      formData.append('files', file)
    })
    formData.append('type', uploadForm.value.type)
    
    const response = await fetch('http://localhost:8088/api/images/upload', {
      method: 'POST',
      credentials: 'include',
      body: formData
    })
    
    const data = await response.json()
    if (data.success) {
      showAlert('上传图片成功', 'success')
      showUploadModal.value = false
      uploadForm.value.files = []
      await fetchImages()
    } else {
      throw new Error(data.message || '上传图片失败')
    }
  } catch (error: any) {
    console.error('上传图片失败:', error)
    showAlert(error.message || '上传图片失败，请重试', 'error')
  } finally {
    isUploading.value = false
  }
}

// 添加提示方法
const showAlert = (message: string, type: 'success' | 'error' | 'warning' | 'info' = 'info') => {
  // 这里可以使用你的提示组件
  console.log(`${type}: ${message}`)
}

onMounted(fetchImages)
</script>

<style scoped>
.image-management-page {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

.page-header h1 {
  font-size: 24px;
  color: rgba(0, 162, 255, 0.9);
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.refresh-btn, .upload-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: none;
  border-radius: 8px;
  background: var(--primary-color);
  color: white;
  cursor: pointer;
  transition: all 0.3s ease;
}

.refresh-btn:hover, .upload-btn:hover {
  background: var(--primary-color-dark);
}

.upload-btn {
  background: rgba(76, 175, 80, 0.9);
}

.upload-btn:hover {
  background: rgba(76, 175, 80, 1);
}

.rotating {
  animation: rotate 1s linear infinite;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.filter-options {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 20px;
  background: var(--bg-primary);
  padding: 16px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.filter-group, .search-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-group select,
.search-group input {
  padding: 8px 12px;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  background: var(--bg-secondary);
  color: var(--text-primary);
}

.search-group {
  flex: 1;
  min-width: 200px;
}

.search-group input {
  flex: 1;
}

.search-group button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 6px;
  background: var(--primary-color);
  color: white;
  cursor: pointer;
}

.images-container {
  background: var(--bg-primary);
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  position: relative;
  min-height: 300px;
  padding: 20px;
}

.images-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 20px;
}

.image-card {
  background: var(--bg-secondary);
  border-radius: 8px;
  overflow: hidden;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.image-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.15);
}

.image-preview {
  height: 150px;
  overflow: hidden;
  cursor: pointer;
}

.image-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.image-preview img:hover {
  transform: scale(1.05);
}

.image-info {
  padding: 10px;
}

.image-name {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 5px;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.image-details {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--text-secondary);
}

.image-actions {
  display: flex;
  padding: 10px;
  background: var(--bg-secondary);
  border-top: 1px solid var(--border-color);
}

.copy-btn, .delete-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px;
  border: none;
  background: none;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.3s ease;
}

.copy-btn:hover {
  color: var(--primary-color);
}

.delete-btn:hover {
  color: #f44336;
}

.no-images {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  color: var(--text-secondary);
}

.no-images-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.loading-container {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.8);
  z-index: 10;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid rgba(0, 0, 0, 0.1);
  border-left-color: var(--primary-color);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 上传区域样式 */
.upload-area {
  border: 2px dashed var(--border-color);
  border-radius: 8px;
  padding: 20px;
  min-height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 16px;
  transition: all 0.3s ease;
}

.upload-area.active {
  border-color: var(--primary-color);
  background: rgba(33, 150, 243, 0.05);
}

.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  color: var(--text-secondary);
}

.upload-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.select-files {
  color: var(--primary-color);
  text-decoration: underline;
  cursor: pointer;
}

.selected-files {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 16px;
  width: 100%;
}

.selected-file {
  position: relative;
  background: var(--bg-secondary);
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.selected-file-preview {
  height: 100px;
  overflow: hidden;
}

.selected-file-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.selected-file-info {
  padding: 8px;
}

.selected-file-name {
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 4px;
}

.selected-file-size {
  font-size: 10px;
  color: var(--text-secondary);
}

.remove-file-btn {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.5);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  cursor: pointer;
  font-size: 10px;
  z-index: 2;
}

/* 模态框样式 */
.modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: var(--bg-primary);
  border-radius: 12px;
  padding: 20px;
  width: 90%;
  max-width: 600px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.preview-modal-content {
  background: var(--bg-primary);
  border-radius: 12px;
  width: 90%;
  max-width: 800px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.preview-image-container {
  flex: 1;
  max-height: 70vh;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #000;
}

.preview-image-container img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.preview-info {
  padding: 16px;
  background: var(--bg-primary);
}

.preview-name {
  font-size: 18px;
  font-weight: 500;
  margin-bottom: 10px;
  color: var(--text-primary);
}

.preview-details {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 16px;
}

.preview-detail {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: var(--text-secondary);
}

.preview-actions {
  display: flex;
  gap: 12px;
}

.preview-btn, .preview-delete-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.preview-btn {
  background: var(--primary-color);
  color: white;
}

.preview-delete-btn {
  background: #f44336;
  color: white;
}

.modal-content h3 {
  margin: 0 0 20px;
  color: var(--text-primary);
}

.modal-body {
  margin-bottom: 20px;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  color: var(--text-secondary);
  font-size: 14px;
}

.form-group select {
  width: 100%;
  padding: 10px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--bg-secondary);
  color: var(--text-primary);
}

.form-group select:focus {
  outline: none;
  border-color: var(--primary-color);
  box-shadow: 0 0 0 2px rgba(33, 150, 243, 0.2);
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.cancel-btn, .confirm-btn, .delete-confirm-btn {
  padding: 8px 20px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.cancel-btn {
  background: var(--bg-secondary);
  color: var(--text-secondary);
}

.confirm-btn {
  background: var(--primary-color);
  color: white;
}

.delete-confirm-btn {
  background: #f44336;
  color: white;
}

.confirm-btn:disabled, .delete-confirm-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .filter-options {
    flex-direction: column;
    align-items: stretch;
  }
  
  .preview-modal-content {
    flex-direction: column;
  }
  
  .preview-image-container {
    max-height: 50vh;
  }
}

/* 暗色主题适配 */
:root[class='dark-theme'] .images-container,
:root[class='dark-theme'] .filter-options {
  background: var(--bg-primary-dark);
}

:root[class='dark-theme'] .image-card {
  background: var(--bg-secondary-dark);
}

:root[class='dark-theme'] .loading-container {
  background: rgba(30, 30, 30, 0.8);
}

:root[class='dark-theme'] .modal-content,
:root[class='dark-theme'] .preview-modal-content,
:root[class='dark-theme'] .preview-info {
  background: var(--bg-primary-dark);
}

:root[class='dark-theme'] .form-group select,
:root[class='dark-theme'] .filter-group select,
:root[class='dark-theme'] .search-group input,
:root[class='dark-theme'] .selected-file {
  background: var(--bg-secondary-dark);
  border-color: var(--border-color-dark);
}

:root[class='dark-theme'] .cancel-btn {
  background: var(--bg-secondary-dark);
}

:root[class='dark-theme'] .upload-area.active {
  background: rgba(33, 150, 243, 0.1);
}

:root[class='dark-theme'] .image-actions {
  background: var(--bg-secondary-dark);
  border-color: var(--border-color-dark);
}
</style> 