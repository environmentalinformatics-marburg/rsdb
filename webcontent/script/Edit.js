"use strict";

var Edit = Class.extend({
	
	init: function(id) {
		this.edit = document.getElementById(id);
		this.onchangeFunc = Helper.nothingFunc;
		this.edit.onchange = this.onChange.bind(this);	
	},
	
	clear: function() {
		this.edit.innerHTML = "";
	},
	
	onChange: function() {
		this.onchangeFunc(this.edit.value);
	},
	
	setOnChange: function(func) {
		this.onchangeFunc = func;
	},
	
	value: function(v) {
		if(v===undefined) {
			return this.edit.value;
		} else {
			this.edit.value = v;
			return v;
		}
	},
	
});