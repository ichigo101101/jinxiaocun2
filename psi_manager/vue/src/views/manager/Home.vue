<template>
  <div>
    <div class="card" style="padding: 15px">
      您好，{{ user?.name }}！欢迎使用本系统
    </div>

    <div style="display: flex; grid-gap: 10px; margin: 10px 0">
      <div class="card" style="flex: 1; display: flex; align-items: center">
        <img src="@/assets/imgs/仓储.png" alt="" style="width: 50px">
        <div style="margin-left: 40px">
          <div style="color: #666">存储余量</div>
          <div style="margin-top: 5px; font-size: 20px">{{ dataCount.store }}</div>
        </div>
      </div>
      <div class="card" style="flex: 1; display: flex; align-items: center">
        <img src="@/assets/imgs/库存余额.png" alt="" style="width: 50px">
        <div style="margin-left: 40px">
          <div style="color: #666">库存余额</div>
          <div style="margin-top: 5px; font-size: 20px">{{ dataCount.money }}</div>
        </div>
      </div>
      <div class="card" style="flex: 1; display: flex; align-items: center">
        <img src="@/assets/imgs/采购.png" alt="" style="width: 50px">
        <div style="margin-left: 40px">
          <div style="color: #666">采购订单</div>
          <div style="margin-top: 5px; font-size: 20px">{{ dataCount.stock }}</div>
        </div>
      </div>
      <div class="card" style="flex: 1; display: flex; align-items: center">
        <img src="@/assets/imgs/销售.png" alt="" style="width: 50px">
        <div style="margin-left: 40px">
          <div style="color: #666">销售订单</div>
          <div style="margin-top: 5px; font-size: 20px">{{ dataCount.sale }}</div>
        </div>
      </div>
    </div>

    <div style="margin: 10px 0; display: flex; grid-gap: 10px">
      <div style="flex: 1" class="card">
        <div style="height: 400px" id="line"></div>
      </div>

      <div style="flex: 1" class="card">
        <div style="height: 400px" id="pie"></div>
      </div>
    </div>

    <div style="display: flex; margin: 10px 0">
      <div style="width: 50%;" class="card">
        <div style="margin-bottom: 30px; font-size: 20px; font-weight: bold">公告列表</div>
        <div >
          <el-timeline  reverse slot="reference">
            <el-timeline-item v-for="item in notices" :key="item.id" :timestamp="item.time">
              <el-popover
                  placement="right"
                  width="200"
                  trigger="hover"
                  :content="item.content">
                <span slot="reference">{{ item.title }}</span>
              </el-popover>
            </el-timeline-item>
          </el-timeline>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import * as echarts from 'echarts'
const lineOption = {
  title: {
    text: '最新一周商品销售的趋势图',
    left: 'center'
  },
  tooltip: {
    trigger: 'axis'
  },
  legend: {
    left: 'left'
  },
  xAxis: {
    type: 'category',
    data: []
  },
  yAxis: {
    type: 'value'
  },
  series: [
    {
      name: '金额',
      data: [],
      type: 'line',
      smooth: true
    },
  ]
}

const pieOption = {
  title: {
    text: '库存统计比例图',
    left: 'center'
  },
  tooltip: {
    trigger: 'item'
  },
  legend: {
    orient: 'vertical',
    left: 'left'
  },
  series: [
    {
      type: 'pie',
      center: ['60%', '55%'],
      radius: '50%',
      data: [],
      label: {
        show: true,
        formatter(param) {
          return param.name + ' (' + param.percent + '%)';
        }
      },
      emphasis: {
        itemStyle: {
          shadowBlur: 10,
          shadowOffsetX: 0,
          shadowColor: 'rgba(0, 0, 0, 0.5)'
        }
      }
    }
  ]
}

export default {
  name: 'Home',
  data() {
    return {
      user: JSON.parse(localStorage.getItem('xm-user') || '{}'),
      notices: [],
      dataCount: {}
    }
  },
  mounted() {
    // 折线图
    let linetDom = document.getElementById('line')
    let lineChart = echarts.init(linetDom)

    // 饼图
    let pieDom = document.getElementById('pie')
    let pieChart = echarts.init(pieDom)

    this.$request.get('/goodsCount').then(res => {
      lineOption.xAxis.data = res.data.map(v => v.name)
      lineOption.series[0].data = res.data.map(v => v.value)
      lineChart.setOption(lineOption)
    })

    this.$request.get('/storeCount').then(res => {
      pieOption.series[0].data = res.data
      pieChart.setOption(pieOption)
    })

  },
  created() {
    this.$request.get('/notice/selectAll').then(res => {
      this.notices = res.data || []
    })

    this.$request.get('/dataCount').then(res => {
      this.dataCount = res.data || {}
    })
  }
}
</script>