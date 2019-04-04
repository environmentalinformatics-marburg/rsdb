"use strict";

var CheckboxShowHide = Checkbox.extend({
	
	init: function(id, checked, className) {
		this._super(id, checked);
		this.className = className;
		this.onChange();		
	},
	
	onChange: function() {
		var value = this.checked()?"inline":"none";
		var tools = document.getElementsByClassName(this.className);
		for (var i=0, max=tools.length; i < max; i++) {
			tools.item(i).style.display = value;
		}
		this._super();
	},
	
});