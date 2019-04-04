"use strict";

var Combo = Class.extend({
	
	/*
	paramer is select object or id of select element
	*/
	init: function(e) {
		this.combo = (typeof e)=="string"?document.getElementById(e):e;
		this.onchangeFunc = Helper.nothingFunc;
		this.onErrorFunc = Helper.nothingFunc;
		this.combo.onchange = this.onChange.bind(this);	
	},
	
	clear: function() {
		this.combo.innerHTML = "";
	},
	
	addEntry: function(text, value) {
		if(value===undefined) {
			value = text;
		}
		//console.log(this);
		this.combo.options.add(new Option(text, value));
	},
	
	onChange: function() {
		this.onchangeFunc(this.combo.value);
	},
	
	setOnChange: function(func) {
		this.onchangeFunc = func;
	},
	
	value: function(v) {
		if(v===undefined) {
			return this.combo.value;
		} else {
			this.combo.value = v;
			return v;
		}
	},
	
	disable: function() {
		this.combo.disabled = true;
	},
	
	enable: function() {
		this.combo.disabled = false;
	},
	
	saveValue: function() {
		this.prevValue = this.value();
	},
	
	restoreValue: function() {
		for(var i=0; i<this.combo.options.length; i++) {
			if(this.prevValue === this.combo.options.item(i).value) {
				this.value(this.prevValue);
				break;
			}
		}
	},
	
	onError: function(text) {
		this.onErrorFunc(text);
	},
	
	setOnError: function(func) {
		this.onErrorFunc = func;
	},
	
});