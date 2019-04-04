<template>
    <div class="selectedEntries" v-if="show">
        <div id="container">
        <v-btn @click="show = false;" title="go back to layer explorer map" icon><v-icon>arrow_back</v-icon></v-btn>    
        <h3 class="headline mb-0" style="text-align: center;"> Selected Layers </h3>
        <v-btn @click="show = false;" title="go back to layer explorer map" icon><v-icon>cancel</v-icon></v-btn>
        </div>
        <v-divider class="meta-divider"></v-divider>
        <span v-if="selectedTags.length > 0">by tags <b><span v-for="tag in selectedTags" :key="tag">{{tag}}</span></b> </span>
        <span v-if="searchText !== undefined && searchText.length > 0"> of search <b>{{searchText}}</b></span>
  
        <v-data-table :headers="tableHeaders" :items="entries" class="meta-content" :pagination.sync="pagination" :custom-sort="sortFunc" hide-actions>
            <template slot="items" slot-scope="props">
                <td><v-icon style="user-select: none;" :title="props.item.type">{{getIcon(props.item)}}</v-icon><a v-if="getMapLink(props.item) !== undefined" :href="getMapLink(props.item)" title="view on map" target="_blank"><v-icon style="user-select: none; color:blue;">zoom_in</v-icon></a></td>
                <td><a v-if="getLink(props.item) !== undefined" :href="getLink(props.item)" title="view details" target="_blank">{{props.item.name}}</a></td>
                <td>{{props.item.title !== undefined ? props.item.title : ''}}</td>
                <td>{{props.item.description}}</td>
            </template>
        </v-data-table>

    </div>
</template>

<script>

export default {
  name: 'admin-explorer-selected',
  props: ['entries', 'selectedTags', 'searchText'],
  data() {
    return {
        show: false,
        tableHeaders: [
            { text: "type", align: 'left', value: "type" }, 
            { text: "ID", align: 'left', value: "name" }, 
            { text: "Title", align: 'left', value: "title" }, 
            { text: "Description", align: 'left', value: "description" }, 
        ],
        pagination: {
            rowsPerPage: -1, // all
            sortBy: "title",            
        },
    }
  },
  methods: {
      onShow() {
          this.show = true;
      },
      getIcon(entry) {
          switch(entry.type) {
              case 'RasterDB': return 'collections';
              case 'PointDB': return 'blur_on';
              case 'pointcloud': return 'grain';
              case 'vectordb': return 'category';
              default: return 'device_unknown';
          }
      },
      getLink(entry) {
          switch(entry.type) {
              case 'RasterDB': return '#/layers/rasterdbs/' + entry.name;
              case 'PointDB': return '#/layers/pointdbs/' + entry.name;
              case 'pointcloud': return '#/layers/pointclouds/' + entry.name;
              case 'vectordb': return '#/layers/vectordbs/' + entry.name;
              default: return undefined;
          }
      },
      getMapLink(entry) {
          switch(entry.type) {
              case 'RasterDB': return '#/viewer/' + entry.name;
              case 'PointDB': return entry.associated !== undefined && entry.associated.rasterdb !== undefined ? '#/viewer/' + entry.associated.rasterdb : undefined;
              case 'pointcloud': return entry.associated !== undefined && entry.associated.rasterdb !== undefined ? '#/viewer/' + entry.associated.rasterdb : undefined;
              case 'vectordb': return '#/vectorviewer/' + entry.name;
              default: return undefined;
          }
      },
      sortFunc(items, index, isDescending) {
         console.log("index " + index);
          var compare = new Intl.Collator().compare;
          return items.sort(function(a, b) {
              var x = a[index];
              var y = b[index];
              var cmp = compare(x === undefined ? a.name : x, y === undefined ? b.name : y);
              return isDescending ? -cmp : cmp;
          });
      }
  },
  watch: {
  },
  mounted() {
    this.$parent.$on('admin-explorer-selected-show', this.onShow);
  },
}

</script>

<style scoped>

#container {
  display: flex;
  justify-content: space-between;
}

.selectedEntries {
  position: absolute;
  top: 0;
  right: 0;
  background-color: #fff;
  width: 100%;
  height: 100%;  
}

</style>