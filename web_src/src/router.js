import Vue from 'vue'
import Router from 'vue-router'

import adminOverview from './components/admin-overview.vue'

import adminLayers from './components/admin-layers.vue'
import adminLayersOverview from './components/admin-layers-overview.vue'
import adminRasterdbDetail from './components/admin-rasterdb-detail.vue'
import adminPointcloudDetail from './components/admin-pointcloud-detail.vue'
import adminVoxeldbDetail from './components/admin-voxeldb-detail.vue'
import adminVectordbDetail from './components/admin-vectordb-detail.vue'
import adminPointdbDetail from './components/admin-pointdb-detail.vue'
import adminPoi_groupsDetail from './components/admin-poi_groups-detail.vue'
import adminRoi_groupsDetail from './components/admin-roi_groups-detail.vue'

import adminExplorer from './components/admin-explorer.vue'
import adminViewer from './components/admin-viewer.vue'
import adminUpload from './components/admin-upload.vue'
import adminAccounts from './components/admin-accounts.vue'
import adminTools from './components/admin-tools.vue'
import adminToolsInfo from './components/admin-tools-info.vue'
import adminToolsStatus from './components/admin-tools-status.vue'
import adminToolsTask from './components/admin-tools-task.vue'

import adminFiles from './components/admin-files.vue'
import adminVectorviewer from './components/admin-vectorviewer.vue'

Vue.use(Router)

export default new Router({
  routes: [
    {path: '/', component: adminOverview},
    {path: '/layers', component: adminLayers,
      children: [
        {path: 'overview', component: adminLayersOverview},
        {path: 'rasterdbs/:rasterdb', component: adminRasterdbDetail, props: true},
        {path: 'pointdbs/:pointdb', component: adminPointdbDetail, props: true},
        {path: 'pointclouds/:pointcloud', component: adminPointcloudDetail, props: true},
        {path: 'voxeldbs/:voxeldb', component: adminVoxeldbDetail, props: true},
        {path: 'vectordbs/:vectordb', component: adminVectordbDetail, props: true},
        {path: 'poi_groups/:poi_group', component: adminPoi_groupsDetail, props: true},
        {path: 'roi_groups/:roi_group', component: adminRoi_groupsDetail, props: true},
        {path: '*', redirect: 'overview' },
        {path: '', redirect: 'overview' },
      ],
    },
    {path: '/explorer', component: adminExplorer},
    {path: '/viewer/:rasterdb?', component: adminViewer, props: (route) => ({ rasterdb: route.params.rasterdb,  timestamp: route.query.timestamp, product: route.query.product, vectordb: route.query.vectordb })},
    {path: '/upload', component: adminUpload}, 
    {path: '/tools', component: adminTools,
      children: [
        {path: 'info', component: adminToolsInfo},
        {path: 'status', component: adminToolsStatus},
        {path: 'task', component: adminToolsTask},
        {path: 'accounts', component: adminAccounts},
        {path: 'vectorviewer/:vectordb?', component: adminVectorviewer, props: true},
        {path: '*', redirect: 'info' },
        {path: '', redirect: 'info' },
      ],    
    },
    {path: '/files', component: adminFiles},  
    {path: '/vectorviewer/:vectordb?', component: adminVectorviewer, props: true},
    //{path: '/vectorviewer/:vectordb?', redirect: to => {console.log(to); return { path: '/viewer', query: {vectordb: to.params.vectordb} };}},
    {path: '*', redirect: '/' },
  ]
})
