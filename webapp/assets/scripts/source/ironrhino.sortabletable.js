(function($) {

	$.fn.sortableTable = function() {
		return this.each(function() {
					SortableTable.init(this);
				});
	};

	var SortableTable = {
		init : function(table, o) {
			if (!table.id)
				table.id = "sortable-table-" + SortableTable._count++;
			$.extend(SortableTable.options, o || {});
			var cells = SortableTable.getHeaderCells(table);
			$(cells).each(function() {
				if (!$(this).hasClass(SortableTable.options.nosortClass)) {
					if (!$('div.sort', this).length) {
						$(this)
								.addClass(SortableTable.options.columnClass)
								.prepend('<div class="sort"><span class="sort"></span></div>');
						$('div.sort', this).click(function() {
									SortableTable._sort.apply(this.parentNode)
								});
					}
				}
			});
			$(SortableTable.getBodyRows(table)).each(function(i) {
						SortableTable.addRowClass(this, i);
					});
		},
		_sort : function(e) {
			SortableTable.sort(null, this);
		},
		sort : function(table, index, order) {
			var cell;
			if (typeof index == 'number') {
				if (!table || ($(table).prop('tagName') != "TABLE"))
					return;
				index = Math.min(table.rows[0].cells.length, index);
				index = Math.max(1, index);
				index -= 1;
				cell = (table.tHead && table.tHead.rows.length > 0)
						? $(table.tHead.rows[table.tHead.rows.length - 1].cells[index])
						: $(table.rows[0].cells[index]);
			} else {
				cell = index;
				table = table ? $(table) : table = $(cell).closest('table')
						.get(0);
				index = SortableTable.getCellIndex(cell)
			}
			var op = SortableTable.options;

			// if(cell.hasClassName(op.nosortClass)) return;
			order = order ? order : ($(cell).hasClass(op.descendingClass)
					? 1
					: -1);

			var hcells = SortableTable.getHeaderCells(null, cell);
			$(hcells).each(function(i) {
				if (i == index)
					if (order == 1)
						$(this).removeClass(op.descendingClass)
								.addClass(op.ascendingClass);
					else
						$(this).removeClass(op.ascendingClass)
								.addClass(op.descendingClass);

				else
					$(this).removeClass(op.ascendingClass)
							.removeClass(op.descendingClass);

			});

			var rows = $.makeArray(SortableTable.getBodyRows(table));
			var datatype = SortableTable.getDataType(cell, index, table);
			rows.sort(function(a, b) {
						return order
								* SortableTable.types[datatype](SortableTable
												.getCellText(a.cells[index]),
										SortableTable
												.getCellText(b.cells[index]));
					});

			$(rows).each(function(i) {
						table.tBodies[0].appendChild(this);
						SortableTable.addRowClass(this, i);
					});
		},
		types : {
			number : function(a, b) {
				// This will grab the first thing that looks like a number from
				// a
				// string, so you can use it to order a column of various srings
				// containing numbers.
				var calc = function(v) {
					v = parseFloat(v.replace(
							/^.*?([-+]?[\d]*\.?[\d]+(?:[eE][-+]?[\d]+)?).*$/,
							"$1"));
					return isNaN(v) ? 0 : v;
				}
				return SortableTable.compare(calc(a), calc(b));
			},
			text : function(a, b) {
				return SortableTable.compare(a ? a.toLowerCase() : '', b ? b
								.toLowerCase() : '');
			},
			casesensitivetext : function(a, b) {
				return SortableTable.compare(a, b);
			},
			datasize : function(a, b) {
				var calc = function(v) {
					var r = v
							.match(/^([-+]?[\d]*\.?[\d]+([eE][-+]?[\d]+)?)\s?([k|m|g|t]?b)?/i);
					var b = r[1] ? Number(r[1]).valueOf() : 0;
					var m = r[3] ? r[3].substr(0, 1).toLowerCase() : '';
					switch (m) {
						case 'k' :
							return b * 1024;
							break;
						case 'm' :
							return b * 1024 * 1024;
							break;
						case 'g' :
							return b * 1024 * 1024 * 1024;
							break;
						case 't' :
							return b * 1024 * 1024 * 1024 * 1024;
							break;
					}
					return b;
				}
				return SortableTable.compare(calc(a), calc(b));
			},
			date : function(a, b) { // must be standard javascript date format
				if (a && b) {
					return SortableTable.compare(new Date(a), new Date(b));
				} else {
					return SortableTable.compare(a ? 1 : 0, b ? 1 : 0);
				}
			},
			time : function(a, b) {
				var d = new Date();
				var ds = d.getMonth() + "/" + d.getDate() + "/"
						+ d.getFullYear() + " "
				return SortableTable
						.compare(new Date(ds + a), new Date(ds + b));
			},
			currency : function(a, b) {
				a = parseFloat(a.replace(/[^-\d\.]/g, ''));
				b = parseFloat(b.replace(/[^-\d\.]/g, ''));
				return SortableTable.compare(a, b);
			}
		},
		compare : function(a, b) {
			return a < b ? -1 : a == b ? 0 : 1;
		},
		detectors : [{
			re : /^sun|mon|tue|wed|thu|fri|sat\,\s\d{1,2}\sjan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec\s\d{4}(?:\s\d{2}\:\d{2}(?:\:\d{2})?(?:\sGMT(?:[+-]\d{4})?)?)?/i,
			type : "date"
		},		// Mon, 18 Dec 1995 17:28:35 GMT
		{
			re : /^\d{1,2}\:\d{2}(?:\:\d{2})?(?:\s[a|p]m)?$/i,
			type : "time"
		},
				// {re: /^[$ï¼â¬î/, type : "currency"}, //
				// dollar,pound,yen,euro,generic currency symbol
				{
					re : /^[-+]?[\d]*\.?[\d]+(?:[eE][-+]?[\d]+)?\s?[k|m|g|t]b$/i,
					type : "datasize"
				}, {
					re : /^[-+]?[\d]*\.?[\d]+(?:[eE][-+]?[\d]+)?$/,
					type : "number"
				}, {
					re : /^[A-Z]+$/,
					type : "casesensitivetext"
				}, {
					re : /.*/,
					type : "text"
				}],
		addSortType : function(name, sortfunc) {
			SortableTable.types[name] = sortfunc;
		},
		addDetector : function(rexp, name) {
			SortableTable.detectors.unshift({
						re : rexp,
						type : name
					});
		},
		getBodyRows : function(table) {
			if (table.tHead && table.tHead.rows.length > 0) {
				return table.tBodies[0].rows;
			} else {
				table.rows.shift();
				return table.rows;
			}
		},
		addRowClass : function(r, i) {
		},
		getHeaderCells : function(table, cell) {
			if (!table)
				table = $(cell).closest('table').get(0);
			return (table.tHead && table.tHead.rows.length > 0)
					? table.tHead.rows[table.tHead.rows.length - 1].cells
					: table.rows[0].cells;
		},
		getCellIndex : function(cell) {
			for (var i = 0; i < cell.parentNode.cells.length; i++)
				if (cell.parentNode.cells[i] == cell)
					return i;
			return -1;
		},
		getCellText : function(cell) {
			if (!cell)
				return "";
			return cell.textContent ? cell.textContent : cell.innerText;
		},
		getDataType : function(cell, index, table) {
			var t;
			var classes = $(cell).attr('class').split(' ');
			for (var i = 0; i < classes.length; i++)
				for (var j = 0; j < SortableTable.types.length; j++)
					if (classes[i] == SortableTable.types[j])
						return SortableTable.types[j];

			var i = index ? index : SortableTable.getCellIndex(cell);
			var tbl = table ? table : $(cell).closest('table').get(0);
			if (tbl.tBodies[0].rows.length == 0)
				return 'text';
			cell = tbl.tBodies[0].rows[0].cells[i]; // grab same index cell from
			// second row to try and match
			// data type
			for (var j; j < SortableTable.detectors.length; j++)
				if (SortableTable.detectors[j].re.test(SortableTable
						.getCellText(cell)))
					return SortableTable.detectors[j].type;
			return 'text';
		},
		setup : function(o) {
			$.extend(SortableTable.options, o || {})
			// in case the user added more types/detectors in the setup options,
			// we
			// read them out and then erase them
			// this is so setup can be called multiple times to inject new
			// types/detectors
			$.extend(SortableTable.types, SortableTable.options.types || {})
			SortableTable.options.types = {};
			if (SortableTable.options.detectors) {
				SortableTable.detectors = SortableTable.options.detectors
						.concat(SortableTable.detectors);
				SortableTable.options.detectors = [];
			}
		},
		options : {
			columnClass : 'sortcol',
			descendingClass : 'sortdesc',
			ascendingClass : 'sortasc',
			nosortClass : 'nosort'
		},
		_count : 0
	};

})(jQuery);

Observation.sortableTable = function(container) {
	$$('table.sortable', container).sortableTable();
};