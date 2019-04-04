"use strict";

var ComboPointdb = Combo.extend({
	
	init: function(id, defaultDbName) {
		this._super(id);
		this.defaultDbName = defaultDbName;	
	},
	
	refresh() {
		this.clear();
		Helper.getJSON('/pointdb/dbs.json',this.onLoadDBs.bind(this));
	},
	
	onLoadDBs: function(db_names) {
		var currDbName = undefined;
		for(var i in db_names) {
			var dbName = db_names[i];
			this.addEntry(dbName);
			if(this.defaultDbName === dbName) {
				currDbName = dbName;
			}
		}
		this.value(currDbName);		
		this.onChange();	
	},
	
});