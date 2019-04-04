"use strict";

var Checkbox = Class.extend({
	
	init: function(id, checked) {
		this.checkbox = document.getElementById(id);
		this.checked(checked);
		this.onchangeFunc = Helper.nothingFunc;
		this.checkbox.onchange = this.onChange.bind(this);		
	},
	
	checked: function(c) {
		if(c===undefined) {
			return this.checkbox.checked;
		} else {
			this.checkbox.checked = c;
			return c;
		}		
	},
	
	setOnchange: function(func) {
		this.onchangeFunc = func;
	},
	
	onChange: function() {
		this.onchangeFunc(this.checkbox.checked);
	},
	
});