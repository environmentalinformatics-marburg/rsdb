<template>
  <v-expansion-panel>
    <v-expansion-panel-content>
      <div slot="header">format description</div>
      <hr>
      <h3 :class="{disabled: (outputType !== 'LAS')}">LAS</h3>
      <br>
      LAS point file format
      <br>
      <br><a href="https://www.asprs.org/committee-general/laser-las-file-format-exchange-activities.html">LAS</a> version 1.2 Point Data Format 0
      <br>
      <br>Tested with: <a href="https://www.liblas.org/">libLAS</a>
      <br>
      <br>     
      <hr>
      <h3 :class="{disabled: (outputType !== 'xyz')}">xyz</h3>
      <br>
      xyz text based point format.
      <br>      
      <br>
      <hr>
      <h3 :class="{disabled: (outputType !== 'rDAT')}">rDAT</h3>
      <br>
      rDAT file format is transfer format of rPointDB package. 
      <br>
      Extensive meta data is preserved by this format.
      <br>e.g. projection
      <br>
      <br>
      example: 
      <br><i>load file in <a href="https://www.r-project.org/">R</a> as data.frame and plot it.</i>
      <br>
      <br>
      <pre>
library(rPointDB)
df &lt;- read.rdat("points.rdat")
library(rgl)
plot3d(df$x, df$y, df$z)
      </pre>
    </v-expansion-panel-content>
  </v-expansion-panel>
</template>

<script>

export default {
  name: 'admin-viewer-point-export-format-description',

  props: ['outputType'],

  components: {
  },

  data() {
    return {};
  },
  methods: {
  },
  computed: {
  },
  watch: {
  },
  mounted() {
  },
}

</script>

<style scoped>

.disabled {
  color: grey;
}

</style>
