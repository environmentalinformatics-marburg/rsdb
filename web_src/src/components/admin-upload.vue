<template>
<div class="innergrid-container">
    <div class="innergrid-item-main">

    <div v-if="uploadState === 'init'">

    <uploader :options="options" class="uploader-example" @file-success="inspectFilename = $event.name; uploadState = 'done';">
      <uploader-unsupport></uploader-unsupport>
      <uploader-drop>
        <p><b>Upload</b> - Drop raster-file here or</p>
        <uploader-btn><v-icon>cloud_upload</v-icon>select raster-file</uploader-btn>
      </uploader-drop>
      <uploader-list></uploader-list>
    </uploader>
    <div style="padding: 50px;">
      <div v-if="identity !== undefined && !identity.secure">
      <h2>For upload of big files you need to connect with HTTPS.</h2>
      You may switch to HTTPS at <a href="#/tools">tools-tab</a>
      <br>
      <br>
      <br>
      </div>
      <p><b>Procedure of raster file-import:</b></p>
      <ul>
      <li>user uploads a raster-file</li>
      <li>remote sensing database inspects raster-file</li>
      <li>remote sensing database prefills import-form</li>
      <li>user completes import-form</li>
      <li>remote sensing database imports raster-file based on import-form</li>
      </ul>
    </div>

    </div>

    <div v-if="uploadState === 'done'">
      <b>raster file uploaded.</b>   <v-btn v-show="importState !== 'done'" @click="cancel_upload()"><v-icon>cancel</v-icon> (cancel import)</v-btn> <v-btn v-show="importState === 'done'" @click="cancel_upload()"><v-icon>arrow_right_alt</v-icon> leave and upload another file</v-btn>
      <hr>
      <table>
      <tr><td><b>filename:</b></td><td>{{inspectFilename}}</td></tr>
      <tr><td><b>timestamp:</b></td><td style="white-space: nowrap"><label for="checkbox"><input type="checkbox" id="checkbox" v-model="guessTimestamp" style="width: 40px;"/>  Try to guess timestamp from filename.</label></td></tr>
      <tr>
        <td style="min-width: 150px;"><b>strategy:</b></td>
        <td><multiselect v-model="importStrategy" :options="importStrategies" track-by="id" label="title" :allowEmpty="false" :searchable="false" :show-labels="false" placeholder="select import strategy" style="min-width: 550px;" /></td>
      </tr>
      <tr v-if="importStrategy.id === 'existing_add' || importStrategy.id === 'existing_merge'">
        <td><b>existing raster layer id:</b></td>
        <td><multiselect v-model="existingRasterdb" :options="rasterdbs" track-by="name" label="name" :allowEmpty="false" :searchable="false" :show-labels="false" placeholder="select import strategy" /></td>
      </tr>
      </table>
      <br><v-btn :disabled="inspectState === 'inspecting' || importState === 'importing' || ((importStrategy.id === 'existing_add' || importStrategy.id === 'existing_merge') && existingRasterdb.name === undefined)" @click="inspect()"><v-icon>zoom_in</v-icon>inspect</v-btn> <span v-show="inspectState === 'done'"><v-icon style="color: grey;" title="note">event_note</v-icon> press again to discard and reload meta data</span>
      <div v-if="inspectState === 'inspecting'">
        <pulse-loader />
        Inspecting file ...
      </div>      
      <hr>
      <div v-if="inspectState === 'done'">
      <br>
      <div>
        <span v-if="specification.gdal_driver !== undefined">file type: <b>{{specification.gdal_driver}}</b> ; </span>
        <b>{{specification.x_range}} width</b> x <b>{{specification.y_range}} height</b> ==> <b>{{specification.x_range * specification.y_range}} pixels</b> ; 
        <span v-if="specification.bands.length <= 1"><b>{{specification.bands.length}} band</b></span>
         <span v-if="specification.bands.length > 1"><b>{{specification.bands.length}} bands</b> ==> <b>{{specification.x_range * specification.y_range * specification.bands.length}} values</b></span>
        <br>
        <br>
        <span style="white-space: nowrap; display: inline-block;"><span style="white-space: nowrap; display: inline-block;">mode </span> <multiselect v-model="importMode" :options="importModes" track-by="id" label="title" :allowEmpty="false" :searchable="false" :show-labels="false" placeholder="select import mode" style="min-width: 550px; white-space: nowrap; display: inline-block;" /></span>
        <br>
        <br>
        <table v-if="importMode.id === 'simple'">
        <tr>
          <td>(optional) <b>timestamp</b> of inserted data within this layer &nbsp;&nbsp;</td>
          <td><input v-model="specification.timestamp" placeholder="timestamp" /></td>
          <td>&nbsp;&nbsp; <v-icon style="color: grey;" title="note">event_note</v-icon>Format: <b>yyyy-MM-dd'T'HH:mm</b> e.g. <span style="color: grey;">2001-12-31T23:59</span> or shortened forms e.g. <span style="color: grey;">2001-12-31</span> or <span style="color: grey;">2001-12</span> or <span style="color: grey;">2001</span></td>
        </tr>           
        </table>
      </div>
      <br>
      <table v-if="create">
        <tr><td><b>filename:</b></td><td>{{specification.filename}}</td></tr>
        <tr><td><b>ID</b></td>
          <td><input v-model="specification.id" placeholder="id" style="min-width: 500px;" /></td>
          <td v-if="!existingID"><v-icon style="color: grey;" title="note">event_note</v-icon><b>Caution</b> with existing IDs: Layer will be deleted and overwritten with imported data.</td>
          <td v-if="existingID" style="color: red;"><v-icon style="color: red;">warning</v-icon> ID already exists: Layer will be deleted and overwritten with imported data.</td>
        </tr>
        <tr><td><b>title</b></td><td><input v-model="specification.title" placeholder="title" /></td></tr>
        <tr><td><b>description</b></td><td><input v-model="specification.description" placeholder="description" /></td></tr>
        <tr><td><b>informal acquisition date</b></td><td><input v-model="specification.acquisition_date" placeholder="date or range of dates" /></td></tr>
        <tr><td><b>corresponding contact</b></td><td><input v-model="specification.corresponding_contact" placeholder="e.g. user name (name@mail.com), user2 name2 (name@mail.com)" /></td></tr>
        <tr><td><b>tags</b></td><td>
          <multiselect v-model="specification.tags" :options="layer_tags" multiple :taggable="true" @tag="createTag" placeholder="select tags" tagPlaceholder="Press enter to create a tag"/>
        </td></tr>
        <tr><td><b>access roles</b></td><td>
          <multiselect v-model="specification.acl" :options="acl_roles" multiple :taggable="true" @tag="createAclRole" placeholder="select roles" tagPlaceholder="Press enter to create a role"/>
        </td>
        <td><v-icon style="color: grey;" title="note">event_note</v-icon> select a role that that you <i>have</i>, to view uploaded (meta-)data (if you are not admin).</td>
        </tr>
        <tr><td><b>modify roles</b></td><td>
          <multiselect v-model="specification.acl_mod" :options="acl_roles" multiple :taggable="true" @tag="createAclModRole" placeholder="select roles" tagPlaceholder="Press enter to create a role"/>
        </td>
        <td><v-icon style="color: grey;" title="note">event_note</v-icon> select a role that that you <i>have</i>, to modify uploaded (meta-)data (if you are not admin).</td>
        </tr>
      </table>

      <table v-if="!create">
        <tr><td><b>filename:</b></td><td>{{specification.filename}}</td></tr>
        <tr><td><b>ID</b></td><td>{{specification.id}}</td></tr>
        <tr><td><b>title</b></td><td>{{specification.title}}</td></tr>
        <tr><td><b>description</b></td><td>{{specification.description}}</td></tr>
        <tr><td><b>acquisition date</b></td><td>{{specification.acquisition_date}}</td></tr>
        <tr><td><b>tags</b></td><td>{{specification.tags}}</td></tr>
        <tr><td><b>access roles</b></td><td>{{specification.acl}}</td></tr>
        <tr><td><b>modify roles</b></td><td>{{specification.acl_mod}}</td></tr>
      </table>

      <table v-if="create">        
        <tr><td><b>pixel size:</b></td><td>x {{specification.pixel_size_x}} y {{specification.pixel_size_y}}</td></tr>
        <tr><td><b>offset:</b></td><td>x {{specification.rasterdb_geo_offset_x}} y {{specification.rasterdb_geo_offset_y}}</td></tr>
      </table>

      <table v-if="!create" style="text-align: center;">
        <tr><td></td><td><b>x</b></td><td>&nbsp;&nbsp;</td><td><b>y</b></td></tr>
        <tr><td><b>layer pixel size&nbsp;&nbsp;</b></td><td>{{specification.pixel_size_x}}</td><td></td><td>{{specification.pixel_size_y}}</td></tr>
        <tr><td><b>file pixel size&nbsp;&nbsp;</b></td><td>{{specification.file_pixel_size_x}}</td><td></td><td>{{specification.file_pixel_size_y}}</td></tr>
      </table>
      <div v-if="specification.pixel_size_x != specification.file_pixel_size_x || specification.pixel_size_y != specification.file_pixel_size_y" style="color: red;">
        <v-icon style="color: red;">warning</v-icon>
        Pixel size of layer raster and pixel size of file raster do seem to differ.
        <br>If difference is more than a very small amount pixels are inserted at wrong positions.
        <br><b style="font-size: 1.2em;">No reprojection will be done.</b>
      </div>
      <br>
      <table v-if="create">
        <tr><td><b>code:</b></td><td><input v-model="specification.geo_code" placeholder="code" /></td><td v-if="validEPSG">==> go to <a :href="'https://epsg.io/' + epsg" target="_blank">epsg.io</a></td><td v-if="!validEPSG"><b>no EPSG code</b>: typical geo codes are of form e.g. <i>EPSG:4326</i></td></tr>
        <tr><td><b>proj4:</b></td><td><input v-model="specification.proj4" placeholder="proj4" style="min-width: 500px;" /></td><td v-if="proj4CompareOK"><b style="color: green;"><v-icon>check</v-icon></b> (checked on epsg.io)</td><td v-if="(!proj4CompareOK) && proj4Compare !== undefined"><b><v-icon>warning</v-icon> Does not match to epsg.io</b> <v-btn @click="specification.proj4 = proj4Compare">==> adopt epsg.io</v-btn> "{{proj4Compare}}"</td><td v-if="proj4CompareError !== undefined"><b>ERROR</b>: could not get proj4 from epsg.io (invalid EPSG code?)</td></tr>
      </table>

      <table v-if="!create">
        <tr><td><b>layer code:</b></td><td>{{specification.geo_code}}</td></tr>
        <tr><td><b>file code:</b></td><td>{{specification.file_geo_code}}</td></tr>
        <tr><td><b>layer proj4:</b></td><td>{{specification.proj4}}</td></tr>
        <tr><td><b>file proj4:</b></td><td>{{specification.file_proj4}}</td></tr>
      </table>
      <div v-if="specification.geo_code != specification.file_geo_code || specification.proj4 != specification.file_proj4" style="color: red;">
        <v-icon style="color: red;">warning</v-icon>
        Projection of layer raster and projection of file raster do seem to differ.
        <br>Check if this is correct.
        <br><b style="font-size: 1.2em;">No reprojection will be done.</b>
      </div>
      <br>
      <div v-if="!create">
        <b>existing band indices:</b>&nbsp;&nbsp;&nbsp;<span style="font-size: 1.5em;">{{specification.existing_bands.map(function(band) {return band.index;})}}</span>
        <br>
        <br>
      </div>      
      <hr>
      <div v-if="importMode.id === 'simple'">
        <div v-for="band in specification.bands" :key="band.file_band_index">
          
        <table>
          <tr>
            <td><b>file band index <span style="font-size: 1.5em;">{{band.file_band_index}}</span> ==> </b></td>
            <td><span>layer band index</span><input v-model="band.rasterdb_band_index" placeholder="band number in rasterdb. Leave empty to omit (not import) this band."  style="min-width: 500px;" /></td>
            <td v-if="band.rasterdb_band_index !== '' && !isNaturalNumber(band.rasterdb_band_index)"><b style="color: red;">Error:</b> band index needs to be a non-negative integer number</td>
            <td v-if="band.rasterdb_band_index !== ''"><v-icon style="color: grey;" title="note">event_note</v-icon> Leave empty to omit (not import) this band.</td>
            <td v-if="band.rasterdb_band_index === ''">band exlcuded from import</td>
          </tr>
          <tr v-if="!create && isNaturalNumber(band.rasterdb_band_index)">
            <td><b>import</b></td>
            <td><b>{{existing_bands_map[band.rasterdb_band_index] === undefined ? 'create new band' : 'into existing band'}}</b></td>
          </tr>
          <tr v-if="isNaturalNumber(band.rasterdb_band_index)">
            <td><b>file band data type ({{band.gdal_raster_data_type}}) ==> </b></td>
            <td v-if="existing_bands_map[band.rasterdb_band_index] === undefined"><v-select v-model="band.rastedb_band_data_type" :items="['short', 'float']" /></td>
            <td v-if="existing_bands_map[band.rasterdb_band_index] !== undefined">{{band.rastedb_band_data_type}}</td>
          </tr>
          <tr v-if="isNaturalNumber(band.rasterdb_band_index)">
            <td><b>file no data value: </b></td>
            <td><input v-model="band.no_data_value" placeholder="no data value" /></td>
          </tr>
          <tr v-if="isNaturalNumber(band.rasterdb_band_index)">
            <td><b>band name:</b></td>
            <td v-if="existing_bands_map[band.rasterdb_band_index] === undefined"><input v-model="band.band_name" placeholder="name" style="min-width: 500px;" /></td>
            <td v-if="existing_bands_map[band.rasterdb_band_index] !== undefined">{{band.band_name}}</td>
          </tr>
          <tr v-if="isNaturalNumber(band.rasterdb_band_index)">
            <td><b>wavelength (nm):</b></td>
            <td v-if="existing_bands_map[band.rasterdb_band_index] === undefined"><input v-model="band.wavelength" placeholder="wavelength in nm" style="min-width: 500px;" /></td>
            <td v-if="existing_bands_map[band.rasterdb_band_index] !== undefined">{{band.wavelength}}</td>
          </tr>
          <tr v-if="isNaturalNumber(band.rasterdb_band_index)">
            <td><b>fwhm (nm):</b></td>
            <td v-if="existing_bands_map[band.rasterdb_band_index] === undefined"><input v-model="band.fwhm" placeholder="fwhm in nm" style="min-width: 500px;" /></td>
            <td v-if="existing_bands_map[band.rasterdb_band_index] !== undefined">{{band.fwhm}}</td>
          </tr>
          <tr v-if="isNaturalNumber(band.rasterdb_band_index)">
            <td><b>visualisation:</b></td>
            <td v-if="existing_bands_map[band.rasterdb_band_index] === undefined"><v-select v-model="band.visualisation" :items="['(not specified)','red', 'green', 'blue']" /></td>
            <td v-if="existing_bands_map[band.rasterdb_band_index] !== undefined">{{band.visualisation}}</td>
          </tr>
        </table>
        <hr>
        </div>
      </div>

      <div v-if="importMode.id === 'timeseries'">
        <br>
        &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<admin-upload-generate-timestamps :specification="specification" @timestamp-sequence="fillTimestamps($event)" />
        <br>
        <v-icon style="color: grey;" title="note">event_note</v-icon>timestamp format: <b>yyyy-MM-dd'T'HH:mm</b> e.g. <span style="color: grey;">2001-12-31T23:59</span> or shortened forms e.g. <span style="color: grey;">2001-12-31</span> or <span style="color: grey;">2001-12</span> or <span style="color: grey;">2001</span>
        <br>
        <table border="1" style="border-collapse: collapse;" bordercolor="#aaaaaa" cellpadding="10">
          <tr>
            <td colspan="3" align="center" style="background-color: rgb(196, 205, 213);"><b>file</b></td>
            <td colspan="2" align="center" style="background-color: rgb(189, 206, 184);"><b>layer</b></td>
          </tr>
          <tr>
            <td align="center" style="background-color: #dee7ef;"><b>band</b></td>
            <td align="center" style="background-color: #dee7ef;"><b>type</b></td>
            <td align="center" style="background-color: #dee7ef;"><b>NA-value</b></td>
            <td align="center" style="background-color: rgb(211, 230, 205);"><b>band</b></td>
            <td align="center" style="background-color: rgb(211, 230, 205);"><b>timestamp</b></td>
          </tr>
          <tr v-for="band in specification.bands" :key="band.file_band_index">
            <td align="center" style="background-color: rgb(244, 250, 255);">{{band.file_band_index}}</td>
            <td align="center" style="background-color: rgb(244, 250, 255);">{{band.gdal_raster_data_type}}</td>
            <td align="center"><input v-model="band.no_data_value" placeholder="no data value" style="text-align: center; background-color: rgb(244, 250, 255);" /></td>
            <td align="center"><input v-model="band.rasterdb_band_index" placeholder="omit"  style="min-width: 10px; text-align: center; background-color: rgb(247, 255, 244);" /></td>
            <td align="center"><input v-model="band.timestamp" placeholder="timestamp"  style="min-width: 10px;text-align: center; background-color: rgb(247, 255, 244);" /></td>
          </tr>
        </table>
        <br>
        <hr>
        <div v-for="band in specification.addBands" :key="band.rasterdb_band_index">
          <table>
            <tr>
              <td><b style="font-size: 1.5em;">layer band index <span style="font-size: 1.5em;">{{band.rasterdb_band_index}}</span></b></td>
            </tr>
            <tr v-if="!create && isNaturalNumber(band.rasterdb_band_index)">
              <td><b>import</b></td>
              <td><b>{{existing_bands_map[band.rasterdb_band_index] === undefined ? 'create new band' : 'into existing band'}}</b></td>
            </tr>
            <tr v-if="isNaturalNumber(band.rasterdb_band_index)">
              <td><b>layer band data type</b></td>
              <td v-if="existing_bands_map[band.rasterdb_band_index] === undefined"><v-select v-model="band.rastedb_band_data_type" :items="['short', 'float']" /></td>
              <td v-if="existing_bands_map[band.rasterdb_band_index] !== undefined">{{band.rastedb_band_data_type}}</td>
            </tr>
            <tr v-if="isNaturalNumber(band.rasterdb_band_index)">
              <td><b>band name:</b></td>
              <td v-if="existing_bands_map[band.rasterdb_band_index] === undefined"><input v-model="band.band_name" placeholder="name" style="min-width: 500px;" /></td>
              <td v-if="existing_bands_map[band.rasterdb_band_index] !== undefined">{{band.band_name}}</td>
            </tr>
            <tr v-if="isNaturalNumber(band.rasterdb_band_index)">
              <td><b>wavelength (nm):</b></td>
              <td v-if="existing_bands_map[band.rasterdb_band_index] === undefined"><input v-model="band.wavelength" placeholder="wavelength in nm" style="min-width: 500px;" /></td>
              <td v-if="existing_bands_map[band.rasterdb_band_index] !== undefined">{{band.wavelength}}</td>
            </tr>
            <tr v-if="isNaturalNumber(band.rasterdb_band_index)">
              <td><b>fwhm (nm):</b></td>
              <td v-if="existing_bands_map[band.rasterdb_band_index] === undefined"><input v-model="band.fwhm" placeholder="fwhm in nm" style="min-width: 500px;" /></td>
              <td v-if="existing_bands_map[band.rasterdb_band_index] !== undefined">{{band.fwhm}}</td>
            </tr>
            <tr v-if="isNaturalNumber(band.rasterdb_band_index)">
              <td><b>visualisation:</b></td>
              <td v-if="existing_bands_map[band.rasterdb_band_index] === undefined"><v-select v-model="band.visualisation" :items="['(not specified)','red', 'green', 'blue']" /></td>
              <td v-if="existing_bands_map[band.rasterdb_band_index] !== undefined">{{band.visualisation}}</td>
            </tr>
          </table>
          <hr>
        </div>

      </div>

      <!--<br>
      <br> {{inspector}}
      <br>
      <br>{{specification}}-->
      <br>
      <v-checkbox label="update pyramid" v-model="specification.update_pyramid" />
      <v-checkbox label="update catalog" v-model="specification.update_catalog" />

      <div v-if="create && (specification === undefined || specification.acl === undefined || specification.acl.length === 0)" style="color: red;">
        <v-icon style="color: red;">warning</v-icon> No access role specified. ==> Only admin can view imported raster.
      </div>
      <div v-if="create && (specification === undefined || specification.acl_mod === undefined || specification.acl_mod.length === 0)" style="color: red;">
        <v-icon style="color: red;">warning</v-icon> No modify role specified. ==> Only admin can modify imported raster and reimport raster with same id.
      </div>
      <br><v-btn  :disabled="importState === 'importing' || (!create && importState !== 'init')" @click="start_import()"><v-icon>cloud_upload</v-icon> &gt;&gt; import &lt;&lt;</v-btn> <span v-show="importState === 'done' && create"><v-icon style="color: grey;" title="note">event_note</v-icon> press again to reimport (e.g. with changed meta data)</span>
 
      <div v-if="remote_task !== undefined">
        {{(remote_task.runtime / 1000).toFixed(0)}} s
        <br><span style="color: grey;">{{remote_task.message}}</span>
      </div>
       <div v-if="importState === 'init'">
        Import not started. Press 'import' to start.
      </div>

      <div v-if="importState === 'importing'">
        <pulse-loader />
        Import is running ...
      </div>

      <div v-if="importState === 'error'">
        <v-icon style="color: red;">error</v-icon> <b>Import error</b>
        <br>{{importErrorStatus}} : {{importErrorStatusText}}
        <br>
        <br>{{importErrorStatusMessage}}
      </div>

      <div v-if="importState === 'done'">
        <v-icon style="color: green;">cloud_done</v-icon> Import done.
        <br><br>view meta data: <a :href="'#/layers/rasterdbs/' + specification.id">{{specification.id}}</a>
        <br><br>view layer: <a :href="'#/viewer/' + specification.id">{{specification.id}}</a>
      </div>
 
 
      </div>

      <br>
      <br>
      <br>

    </div>

    <div v-show="Object.keys(messages).length > 0" class="message_box">
      <div v-for="(text, id) in messages" :key="id">{{text}}</div>
    </div>
    <v-snackbar value="true"  v-for="(text, id) in notifications" :key="id">
      {{text}}<v-btn flat color="pink" @click.native="removeNotification(id)">Close</v-btn>
    </v-snackbar>
  </div>
  </div>  
</template>

<script>

import { mapState } from 'vuex'

import Vue from 'vue'
import axios from 'axios'
import PulseLoader from 'vue-spinner/src/PulseLoader.vue'
import uploader from 'vue-simple-uploader'

import Multiselect from 'vue-multiselect'
import 'vue-multiselect/dist/vue-multiselect.min.css'

import adminUploadGenerateTimestamps from './admin-upload-generate-timestamps.vue'

Vue.use(uploader)

// derived from https://stackoverflow.com/questions/9229645/remove-duplicate-values-from-js-array
function unique(a) {
    return a.slice().sort((a, b) => a - b).filter(function(item, pos, ary) {
        return !pos || item != ary[pos - 1];
    })
}

export default {
  name: 'admin-upload',

  props: [],

  components: {
    PulseLoader,
    Multiselect,
    'admin-upload-generate-timestamps': adminUploadGenerateTimestamps,
  },

  data() {
    return {
      messages: {},
      messagesIndex: 0,
      notifications: {},
      notificationsIndex: 0,

      options: {
          // https://github.com/simple-uploader/Uploader/tree/develop/samples/Node.js
          target: '../../api/upload',
          testChunks: false,
          chunkSize: 10*1024*1024,
          simultaneousUploads: 1,
        },

      uploadState: "init",

      inspectState: "init",
      inspectFilename: undefined,
      guessTimestamp: true,
      importStrategies: [{id: 'create', title: 'create new layer'}, 
                          {id: 'existing_add', title: 'insert into existing layer with new (added) bands'}, 
                          {id: 'existing_merge', title: 'insert into existing layer and (preferably) into exisiting bands'}],
      importStrategy: {},
      importModes: [{id: 'simple', title: 'simple (file represents one point in time)'}, 
                    {id: 'timeseries', title: 'timeseries (bands of file represent multiple points in time)'}], 
      importMode: {},      
      existingRasterdb: {},
      specification: undefined,
      proj4Compare: undefined,
      proj4CompareError: undefined,
      createdTags: [],
      createdAclRoles: [],

      importState: "init",
      importResult: undefined,
      importErrorStatus: undefined,
      importErrorStatusText: undefined,
      importErrorStatusMessage: undefined,

      presetBands: [],
      presetTimestamp: undefined,

      remote_task: undefined,
    }
  },
  methods: {
    createTag(newTag) {
      this.createdTags.push(newTag);
      this.specification.tags.push(newTag);
    },
    createAclRole(newAclRole) {
      this.createdAclRoles.push(newAclRole);
      this.specification.acl.push(newAclRole);
    },
    createAclModRole(newAclModRole) {
      this.createdAclRoles.push(newAclModRole);
      this.specification.acl_mod.push(newAclModRole);
    },
    addnotification(text) {
      var id = this.notificationsIndex++;
      Vue.set(this.notifications, id, text);
      return id;
    },
    removeNotification(id) {
      Vue.delete(this.notifications, id);
    },
    addMessage(text) {
      var id = this.messagesIndex++;
      Vue.set(this.messages, id, text);
      return id;
    },
    removeMessage(id) {
      Vue.delete(this.messages, id);
    },

    cancel_upload() {
      this.uploadState = "init";

      this.inspectState = "init";
      this.inspectFilename = undefined;
      this.specification = undefined;
      this.proj4Compare = undefined;
      this.proj4CompareError = undefined;

      this.importState = "init";
      this.importResult = undefined;
    },
    
    inspect() {
      var self = this;
      this.importState = "init";
      this.remote_task = undefined;
      self.inspectState = 'inspecting';
      self.specification = undefined;
      var params = {filename: this.inspectFilename, strategy: this.importStrategy.id};
      if(this.importStrategy.id == 'existing_add' || this.importStrategy.id == 'existing_merge') {
        params.rasterdb = this.existingRasterdb.name;
      }
      if(this.guessTimestamp) {
        params.guess_timestamp = true;
      }
      axios.get(this.urlPrefix + '../../api/inspect', {params: params})
      .then(function(response) {
          self.inspectState = 'done';
          self.specification = response.data.specification;
          self.presetBands = JSON.parse(JSON.stringify(self.specification.bands));
          self.setPresetBands();
          self.presetTimestamp = self.specification.timestamp;
       })
      .catch(function(error) {
        self.inspectState = 'error';
        self.addnotification("ERROR " + error);
      });
    },

    start_import() {
      var self = this;
      this.remote_task = undefined;
      var url = this.$store.getters.apiUrl('api/import');
      self.importState = "importing";
      axios.post(
        url, 
        {specification: self.specification,}
      ).then(function(response) {
          /*self.importState = "done";
          console.log(response);
          self.refreshAfterImport();*/
          console.log(response.data.remote_task);
          self.remote_task = response.data.remote_task;
          self.query_remote_task();
      }).catch(function(error) {
          self.importState = "error";
          self.importErrorStatus = "unknown";
          self.importErrorStatusText = "unknown";
          self.importErrorStatusMessage = "unknown";
          if(error.response !== undefined) {
            if(error.response.status !== undefined) {
              self.importErrorStatus = error.response.status;
            }
            if(error.response.statusText !== undefined) {
              self.importErrorStatusText = error.response.statusText;
            }
            if(error.response.data !== undefined) {
              self.importErrorStatusMessage = error.response.data;
            }
          } else {
            self.importErrorStatusMessage = self.errorToText(error);
          }
          
          console.log("1------");
          console.log(JSON.stringify(error));
          console.log("2------");
          console.log(self.errorToText(error));
          console.log("3------");
          self.setError = true;
          self.setErrorMessage = "Error setting property: " + self.errorToText(error);
          console.log(error);
          self.refreshAfterImport();
      });
    },

    refreshAfterImport() {
      this.$store.dispatch('layer_tags/refresh');
      this.$store.dispatch('acl_roles/refresh');
      this.$store.dispatch('rasterdbs/refresh');
    },

    isNaturalNumber(n) { //source: https://stackoverflow.com/questions/16799469/how-to-check-if-a-string-is-a-natural-number
      n = n.toString(); // force the value incase it is not
      var n1 = Math.abs(n),
          n2 = parseInt(n, 10);
      return !isNaN(n1) && n2 === n1 && n1.toString() === n;
    },

    setBand(i) {
      var existingBand = this.existing_bands_map[this.specification.bands[i].rasterdb_band_index];
      this.specification.bands[i].rastedb_band_data_type = existingBand.type;
      this.specification.bands[i].band_name = existingBand.name;
      this.specification.bands[i].wavelength = existingBand.wavelength;
      this.specification.bands[i].fwhm = existingBand.fwhm ;
      this.specification.bands[i].visualisation = existingBand.visualisation;
    },

    clearBand(i) {  
      this.specification.bands[i].rastedb_band_data_type = this.presetBands[i].rastedb_band_data_type;
      this.specification.bands[i].band_name = this.presetBands[i].band_name;
      this.specification.bands[i].wavelength = this.presetBands[i].wavelength;
      this.specification.bands[i].fwhm = this.presetBands[i].fwhm;
      this.specification.bands[i].visualisation = this.presetBands[i].visualisation;      
    },

    setPresetBands() {
      console.log(this.specification);
      console.log(this.specification.bands);
      var len = this.specification.bands.length;
      if(len < 1) {
        return;
      }
      if(this.importMode.id === 'simple') {
        for(var i=0; i<len; i++) {
          this.specification.bands[i].rasterdb_band_index = this.presetBands[i].rasterdb_band_index; 
          this.specification.bands[i].rastedb_band_data_type = this.presetBands[i].rastedb_band_data_type;
          this.specification.bands[i].band_name = this.presetBands[i].band_name;
          this.specification.bands[i].wavelength = this.presetBands[i].wavelength;
          this.specification.bands[i].fwhm = this.presetBands[i].fwhm;
          this.specification.bands[i].visualisation = this.presetBands[i].visualisation;
          this.specification.bands[i].no_data_value = this.presetBands[i].no_data_value;
          this.specification.bands[i].visualisation = this.presetBands[i].visualisation;
        }
      } else if(this.importMode.id === 'timeseries') {
        // eslint-disable-next-line
        for(var i=0; i<len; i++) {
          this.specification.bands[i].rasterdb_band_index = this.presetBands[0].rasterdb_band_index; 
          this.specification.bands[i].rastedb_band_data_type = this.presetBands[0].rastedb_band_data_type;
          this.specification.bands[i].band_name = this.presetBands[0].band_name;
          this.specification.bands[i].wavelength = this.presetBands[0].wavelength;
          this.specification.bands[i].fwhm = this.presetBands[0].fwhm;
          this.specification.bands[i].visualisation = this.presetBands[0].visualisation;
          this.specification.bands[i].no_data_value = this.presetBands[0].no_data_value;
          this.specification.bands[i].visualisation = this.presetBands[0].visualisation;
        }
      } else {
        console.log("error");
      }
    },

    errorToText(error) {
      if(error === undefined) {
          return "unknown error";
      }
      if(error.message === undefined) {
          return error;
      }
      if(error.response === undefined || error.response.data === undefined) {
          return error.message;
      }
      return error.message + " - " + error.response.data;
    },

    query_remote_task() {
      var self = this;
      axios.get(this.urlPrefix + '../../api/remote_tasks/' + this.remote_task.id)
      .then(function(response) {
          console.log(response.data.remote_task);
          self.remote_task = response.data.remote_task;
          if(self.remote_task.active) {
            window.setTimeout(function() {self.query_remote_task();}, 1000);
          } else {
            if(self.remote_task.status === 'DONE') {
              self.importState = "done";
            } else {
              self.importState = "error";
            }
            console.log(response);
            self.refreshAfterImport();
          }
      }).catch(function(error) {
          self.importState = "error";
          self.importErrorStatus = "unknown";
          self.importErrorStatusText = "unknown";
          self.importErrorStatusMessage = "unknown";
          if(error.response !== undefined) {
            if(error.response.status !== undefined) {
              self.importErrorStatus = error.response.status; 
            }
            if(error.response.statusText !== undefined) {
              self.importErrorStatusText = error.response.statusText;
            }
            if(error.response.data !== undefined) {
              self.importErrorStatusMessage = error.response.data;
            }
          } else {
            self.importErrorStatusMessage = self.errorToText(error);
          }
          
          console.log("1------");
          console.log(JSON.stringify(error));
          console.log("2------");
          console.log(self.errorToText(error));
          console.log("3------");
          self.setError = true;
          self.setErrorMessage = "Error setting property: " + self.errorToText(error);
          console.log(error);
          self.refreshAfterImport();
      });
    },

    updateAddBands() {
      var self = this;
      if(this.importMode.id === 'simple') {
        if(this.specification !== undefined) {
          this.specification.addBands = undefined;
        }
        return;
      }
      if(this.specification.addBands === undefined) {
        this.specification.addBands = [];
      }
      this.specification.addBands = this.specification.addBands.filter(function(band) {return self.selectedRasterDbBandIndicesUnique.includes(band.rasterdb_band_index)});

      var toAddIndices = self.selectedRasterDbBandIndicesUnique.filter(function (index) {
        for(var i=0; i<self.specification.addBands.length; i++) {
          if(index === self.specification.addBands[i].rasterdb_band_index) {
            return false;
          }
        }
        return true;
      });

      console.log("toAddIndices");
      console.log(toAddIndices);

      var toAddBands = toAddIndices.map(function(index) {
        var copyBand = {}
        for(var i=0; i<self.specification.bands.length; i++) {
          if(self.specification.bands[i].rasterdb_band_index === index) {
            console.log("found!");
            copyBand = self.specification.bands[i];
            break;
          }
        }
        return {rasterdb_band_index: index,
                band_name: copyBand.band_name,
                fwhm: copyBand.fwhm,
                rastedb_band_data_type: copyBand.rastedb_band_data_type,
                visualisation: copyBand.visualisation,
                wavelength: copyBand.wavelength};
      });

      this.specification.addBands = this.specification.addBands.concat(toAddBands);
    },

    fillTimestamps(timestamps) {
      for(var i=0; i<timestamps.length; i++) {
        console.log("set" + timestamps[i]);
        this.specification.bands[i].timestamp = timestamps[i];
      }
    }

  },
  computed: {
    ...mapState({
      urlPrefix: state => state.identity.urlPrefix,
    }),
    validEPSG() {
      return this.specification !== undefined && this.specification.geo_code !== undefined && this.specification.geo_code.startsWith("EPSG:");
    },
    epsg() {
      return this.validEPSG ? this.specification.geo_code.substr(5) : undefined;
    },
    proj4CompareOK() {
      return this.specification.proj4 === this.proj4Compare;
    },
    layer_tags() {
      return this.$store.state.layer_tags.data === undefined ?  this.createdTags : this.$store.state.layer_tags.data.concat(this.createdTags);
    },
    acl_roles() {
      return this.$store.state.acl_roles.data === undefined ?  this.createdAclRoles : this.$store.state.acl_roles.data.concat(this.createdAclRoles);
    },
    rasterdbs() {
      return this.$store.state.rasterdbs.data === undefined ? [] : this.$store.state.rasterdbs.data;
    },
    existingID() {
      if(this.specification === undefined || this.specification.id === undefined || this.rasterdbs === undefined) {
        return false;
      }
      return this.rasterdbs.some(function(e) {
        return e.name === this.specification.id;
      }, this);
    },
    create() {
      return this.importStrategy.id === 'create';
    },
    existing_bands_map() {
      return this.specification.existing_bands === undefined ? {} : this.specification.existing_bands.reduce(function(map, band) {map[band.index] = band; return map;}, {});
    },
    selectedRasterDbBandIndices() {
      return this.specification === undefined ? [] : this.specification.bands.map(function(band) {return band.rasterdb_band_index;});
    },
    selectedRasterDbBandIndicesUnique() {
      return unique(this.selectedRasterDbBandIndices);
    },
    identity() {
      return this.$store.state.identity.data;
    },
  },
  watch: {
    epsg() {
      var self = this;
      this.proj4Compare = undefined;
      this.proj4CompareError = undefined;
      if(this.epsg !== undefined) {
        axios.get('https://epsg.io/' + this.epsg + ".proj4").then(function(response) {
          self.proj4Compare = response.data;
        }).catch(function(error) {
          console.log(error);
          self.proj4CompareError = JSON.stringify(error);
        });
      }
    },
    importStrategy() {
      this.inspectState = "init";
      this.specification = undefined;
    },
    existingRasterdb() {
      this.inspectState = "init";
      this.specification = undefined;
    },
    selectedRasterDbBandIndices(after, before) {
      console.log(before);
      console.log(after);
      var len = after.length;
      for(var i=0; i<len; i++) {
        //console.log("check "+i+": "+before[i]+" -> "+after[i]);
        if(this.isNaturalNumber(after[i])) {
          if(after[i] !== before[i]) {
            var existingBand = this.existing_bands_map[after[i]];
            if(existingBand !== undefined) {
              this.setBand(i);
            } else {
              var beforeExistingBand = this.existing_bands_map[before[i]];
              if(beforeExistingBand !== undefined) {
                this.clearBand(i);
              }
            }
          }
        } else if(this.existing_bands_map[before[i]] !== undefined) {
          this.clearBand(i);
        }
      }
    },
    remote_task() {
      
    },
    importMode() {
      var self = this;
      if(this.specification !== undefined) {
        this.setPresetBands();
        if(this.importMode.id === 'simple') {
          self.specification.timestamp = self.presetTimestamp;
          for(var i=0; i<this.specification.bands.length; i++) {
            this.specification.bands[i].timestamp = undefined; 
          }
        } else if(this.importMode.id === 'timeseries') {
          self.specification.timestamp = undefined;
        }
      }
      this.updateAddBands();
    },
    selectedRasterDbBandIndicesUnique() {
      this.updateAddBands();
    }
  },
  mounted() {
    this.$store.dispatch('layer_tags/init');
    this.$store.dispatch('acl_roles/init');
    this.$store.dispatch('rasterdbs/init');
    this.$store.dispatch('identity/init');
    this.importStrategy = this.importStrategies[0];
    this.importMode = this.importModes[0];
    var self = this;
    window.injectFile = function(filename){self.inspectFilename = filename;}
  },
}

</script>

<style scoped>

.innergrid-container {
  display: grid;
  grid-template-columns: auto;
  grid-template-rows: auto;
}

.innergrid-item-main {
  padding: 20px;
  overflow-y: auto;
}

.message_box {
  position: absolute;
  top: 200px;
  left: 600px;
  background-color: rgb(243, 243, 243);
  padding: 1px;
  border-color: rgb(137, 137, 137);
  border-style: solid;
  border-width: 1px;
  font-size: 1.5em;
}

  .uploader-example {
    width: 880px;
    padding: 15px;
    margin: 40px auto 0;
    font-size: 12px;
    box-shadow: 0 0 10px rgba(0, 0, 0, .4);
  }
  .uploader-example .uploader-btn {
    margin-right: 4px;
  }
  .uploader-example .uploader-list {
    max-height: 440px;
    overflow: auto;
    overflow-x: hidden;
    overflow-y: auto;
  }

  input {
    width: 100%;
    border: 1px solid #ccc;
    box-shadow: inset 0 1px 3px #ddd;
    border-radius: 4px;
    box-sizing: border-box;
    padding-left: 3px;
    padding-right: 3px;
    padding-top: 3px;
    padding-bottom: 3px;
    background-color: white;
  }

  .disabled {
    color: grey;
  }


</style>


