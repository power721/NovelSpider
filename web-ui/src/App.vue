<template>
  <div class="p-6">
    <!-- Search & Filter Row -->
    <div class="flex items-center gap-4 mb-4">
      <!-- Search Input -->
      <el-input
          v-model="query"
          placeholder="请输入小说名称"
          clearable
          @keyup.enter="handleSearch"
          style="width: 400px"
      >
        <template #append>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
        </template>
      </el-input>
      <!-- Search Input -->
      <el-input
          v-model="author"
          placeholder="作者"
          clearable
          @change="handleSearch"
          style="width: 300px"
      >
      </el-input>

      <!-- Status Filter -->
      <el-select v-model="status" placeholder="选择状态" clearable style="width: 120px" @change="handleSearch">
        <el-option label="全部" value=""/>
        <el-option label="连载" value="连载"/>
        <el-option label="全本" value="全本"/>
      </el-select>

      <!-- Category Filter -->
      <el-select v-model="category" placeholder="选择分类" clearable style="width: 150px" @change="handleSearch">
        <el-option label="全部" value=""/>
        <el-option
            v-for="c in categories"
            :key="c"
            :label="c || '未分类'"
            :value="c"
        />
      </el-select>

      <el-button @click="refresh">刷新</el-button>
    </div>

    <!-- Results Table -->
    <el-table
        v-loading="loading"
        :data="results"
        style="width: 100%"
        border
        @sort-change="handleSort"
    >
      <el-table-column prop="title" label="标题" min-width="200">
        <template #default="{ row }">
          <a :href="row.novelUrl" target="_blank">{{ row.title }}</a>
        </template>
      </el-table-column>
      <el-table-column prop="author" label="作者" width="180">
        <template #default="{ row }">
          <el-link type="primary" @click="filterByAuthor(row.author)">
            {{ row.author }}
          </el-link>
        </template>
      </el-table-column>
      <el-table-column prop="category" label="分类" width="70">
        <template #default="{ row }">
          <el-link type="success" @click="filterByCategory(row.category)">
            {{ row.category || '未分类' }}
          </el-link>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="70"/>
      <el-table-column prop="wordCount" label="字数" width="80" sortable="custom">
        <template #default="{ row }">
          {{row.wordCount / 10000}}万
        </template>
      </el-table-column>
      <el-table-column prop="updatedAt" label="最新更新" width="150" sortable="custom"/>
      <el-table-column prop="description" label="简介" min-width="300" show-overflow-tooltip/>
    </el-table>

    <!-- Pagination -->
    <el-pagination
        v-if="total > 0"
        class="mt-4"
        layout="total, prev, pager, next, jumper"
        :page-size="pageSize"
        :total="total"
        :current-page="page + 1"
        @current-change="handlePageChange"
    />
  </div>
</template>

<script setup>
import {onMounted, ref} from "vue"
import { useDark } from '@vueuse/core'
import axios from "axios"

useDark()

const query = ref("")
const sort = ref("updatedAt,desc")
const status = ref("")
const category = ref("")
const author = ref("")
const results = ref([])
const loading = ref(false)

const page = ref(0)
const pageSize = ref(20)
const total = ref(0)

const categories = ref([
  "科幻", "玄幻", "穿越", "网游",
  "历史", "现代", "惊悚", "美文",
  "言情", "武侠", "管理",
  "学习", "耽美", ""
])

const crawl = () => {
  axios.post('/api/novels/crawl').then()
}

const refresh = () => {
  handleSearch()
}

const handleSort = ({prop, order}) => {
  if (order) {
    sort.value = prop + "," + (order === 'ascending' ? 'asc' : 'desc')
  } else {
    sort.value = "updatedAt,desc"
  }
  handleSearch()
}

const handleSearch = async () => {
  loading.value = true
  try {
    const res = await axios.get("/api/novels/search", {
      params: {
        q: query.value,
        status: status.value,
        author: author.value,
        category: category.value,
        page: page.value,
        size: pageSize.value,
        sort: sort.value
      }
    })
    results.value = res.data.content
    total.value = res.data.totalElements
  } finally {
    loading.value = false
  }
}

const filterByAuthor = (name) => {
  author.value = name
  page.value = 0
  handleSearch()
}

const filterByCategory = (cat) => {
  category.value = cat
  page.value = 0
  handleSearch()
}

const handlePageChange = (newPage) => {
  page.value = newPage - 1
  handleSearch()
}

onMounted(() => {
  handleSearch()
})
</script>

<style scoped>
.mt-4 {
  margin-top: 1rem;
}
</style>
