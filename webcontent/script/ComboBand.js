var ComboBand = Combo.extend({
	
	init: function(id) {
		this._super(id);
	},
	
	refresh: function(db_name) {
		this.clear();
		Helper.getJSON('/spectraldb/band_info.json',this.onLoadBandInfo.bind(this));
	},
	
	onLoadBandInfo: function(bandInfos) {
		for(var i in bandInfos) {
			var row = bandInfos[i];
			this.addEntry(row.name, row.band);
		}
		this.onChange();	
	},
	
});