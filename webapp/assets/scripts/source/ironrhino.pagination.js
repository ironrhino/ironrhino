(function($) {

	$(function() {
				$(document).on('click',
						'.toolbar .pagination .firstPage:not(.disabled) a',
						function(event) {
							var form = $(this).closest('form');
							$('.inputPage', form).val(1);
							form.submit();
							return false;
						}).on('click',
						'.toolbar .pagination .prevPage:not(.disabled) a',
						function(event) {
							var form = $(this).closest('form');
							$('.inputPage', form).val(function(i, v) {
										return parseInt(v) - 1
									});
							form.submit();
							return false;
						}).on('click',
						'.toolbar .pagination .nextPage:not(.disabled) a',
						function(event) {
							var form = $(this).closest('form');
							$('.inputPage', form).val(function(i, v) {
										return parseInt(v) + 1
									});
							form.submit();
							return false;
						}).on('click',
						'.toolbar .pagination .lastPage:not(.disabled) a',
						function(event) {
							var form = $(this).closest('form');
							$('.inputPage', form).val($('.totalPage strong',
									form).text());
							form.submit();
							return false;
						}).on('change', '.toolbar .pagination .inputPage',
						function(event) {
							var form = $(event.target).closest('form');
							form.submit();
							event.preventDefault();
						}).on('change', '.toolbar .pagination select.pageSize',
						function(event) {
							var form = $(event.target).closest('form');
							$('.inputPage', form).val(1);
							form.submit();
						}).on('keydown', '.toolbar input[name="keyword"]',
						function(event) {
							var form = $(event.target).closest('form');
							if (event.keyCode == 13) {
								$('.inputPage', form).val(1);
								form.submit();
								return false;
							}
						}).on('click', '.toolbar .glyphicon-search',
						function(event) {
							var form = $(event.target).closest('form');
							$('.inputPage', form).val(1);
							form.submit();
							return false;
						});

			});
})(jQuery);