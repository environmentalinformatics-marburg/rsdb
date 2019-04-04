var Helper = {

	getJSON: function(url, func, errorFunc) {
		var client = new XMLHttpRequest();
		client.open('GET', url, true);
		client.setRequestHeader ("Accept", "application/json");
		client.onreadystatechange = function() {				
			if (client.readyState == 4) {
				if(client.status == 200) {
					var value = JSON.parse(client.responseText);						
					func(value);
				} else {
					if(errorFunc!==undefined) {
						errorFunc(client.responseText);
					}
				}
			}				
		}
		client.send();	
	},
	
	getText: function(url, func, errorFunc) {
		var client = new XMLHttpRequest();
		client.open('GET', url, true);
		client.setRequestHeader ("Accept", "text/plain");
		client.onreadystatechange = function() {
			if (client.readyState == 4) {
				if(client.status == 200) {
					var value = client.responseText;						
					func(value);
				} else {
					if(errorFunc!==undefined) {
						errorFunc(client.responseText);
					}
				}
			}
		}
		client.send();	
	},
	
	toQuery: function(parameters) {
		var query = "";
		for(var p in parameters) {
			
			if(query.length == 0) {
				query = "?";
			} else {
				query += "&";
			}
			query += p + "=" + parameters[p];
		}
		return query;
	},
	
	merge_properties: function(a,b){
		var c = {};
		for (var attrname in a) { c[attrname] = a[attrname]; }
		for (var attrname in b) { c[attrname] = b[attrname]; }
		return c;
	},
	
	insert_properties: function(o, p) {
		for (var attrname in p) { o[attrname] = p[attrname]; }
		return o;
	},
	
	//http://stackoverflow.com/questions/400212/how-do-i-copy-to-the-clipboard-in-javascript
	copyToClipboard: function(text) {
	  window.prompt("Copy to clipboard: Ctrl+C, Enter", text);
	},
	
	messagebox: function(title, message) {
	  window.prompt(title, message);
	},
	
	nothingFunc: function(){
	},
	
	jsonToTable: function(json) {
		s='<table class="u-full-width">';
		s+='<thead><tr><th>Name</th><th>Value</th></tr></thead>';
		s+='<tbody>';
		
		for(var r in json) {
			s+="<tr>";
			s+="<td>"+r+"</td>";
			s+="<td>"+json[r]+"</td>";
			s+="</tr>";
		}

		s+="</tbody>";
		s+="</table>";
		return s;
	},
	
	jsonArrayToTable: function(jsonArray) {
		if(jsonArray.length==0) {
			return "";
		}
		s='<table class="u-full-width">';
		s+='<thead><tr>';
		
		jsonFirst = jsonArray[0];
		var names = [];
		for(var r in jsonFirst) {
				s+="<th>"+r+"</th>";
				names.push(r);
		}
		
		s+='</tr></thead>';
		
		s+='<tbody>';
		
		for(var i in jsonArray) {
			var json = jsonArray[i];
			s+="<tr>";
			var c=0;
			for(var r in json) {
				if(names[c++]===r) {
				s+="<td>"+json[r]+"</td>";
				} else {
					s+="<td>"+json[r]+" (! in "+r+")</td>";
				}
			}
			s+="</tr>";
		}

		s+="</tbody>";
		s+="</table>";
		return s;
	},

	injectHtml: function() {
		//derived from: (client side includes via javascript) https://github.com/LexmarkWeb/csi.js
		var elements = document.getElementsByTagName('*');
		for (var i in elements) {
			if (elements[i].hasAttribute && elements[i].hasAttribute('data-include')) {
				fragment(elements[i], elements[i].getAttribute('data-include'));
			}
		}
		function fragment(el, url) {
			var localTest = /^(?:file):/,
				xmlhttp = new XMLHttpRequest(),
				status = 0;

			xmlhttp.onreadystatechange = function() {
				/* if we are on a local protocol, and we have response text, we'll assume things were sucessful */
				if (xmlhttp.readyState == 4) {
					status = xmlhttp.status;
				}
				if (localTest.test(location.href) && xmlhttp.responseText) {
					status = 200;
				}
				if (xmlhttp.readyState == 4 && status == 200) {
					el.outerHTML = xmlhttp.responseText;
				}
				if (xmlhttp.readyState == 4 && status != 200) {
					el.outerHTML = "<p>ERROR: page not found: "+url+"</p>";
				}
			}

			try { 
				xmlhttp.open("GET", url, true);
				xmlhttp.send();
			} catch(err) {
				console.log(err);
			}
		}			
	},
	
	meterToText: function(value) {
		if(value>=1) {
			return value+"m";
		} else {
			return (value*100)+"cm";
		}
	},
	
	/*
	Extract parameters from URL
	*/
	//derived from http://www.xul.fr/javascript/parameters.php
	extractParameters: function() {
		var parameters = {};
		var parameterTexts = location.search.substring(1).split("&");
		for(i in parameterTexts) {
			var p = parameterTexts[i].split("=");
			parameters[p[0]] = p[1];				
		}
		return parameters; 
	},
	
	createButton: function(text, func){
		var button = document.createElement("input");
		button.type = "button";
		button.value = text;
		button.onclick = func;
		return button;
	},
}