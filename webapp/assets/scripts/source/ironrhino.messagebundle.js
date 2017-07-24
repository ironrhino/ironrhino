MessageBundle = {
	'en' : {
		'ajax.loading' : 'loading...',
		'ajax.error' : 'network error,please try later',
		'required' : 'this is required',
		'required.only.one' : '{0} are required only one',
		'required.at.least.one' : '{0} are required at least one',
		'selection.required' : 'please select',
		'phone' : 'please input valid phone number',
		'email' : 'please input valid email',
		'regex' : 'please input valid value',
		'integer' : 'must be a integer',
		'integer.positive' : 'must be a positive integer',
		'double' : 'must be a decimal',
		'double.positive' : 'must be a positive decimal',
		'save.and.create' : 'save and add',
		'no.selection' : 'no selection',
		'no.modification' : 'no modification',
		'select' : 'please select',
		'ok' : 'OK',
		'cancel' : 'Cancel',
		'confirm.delete' : 'are you sure to delete?',
		'confirm.save' : 'are you sure to save?',
		'confirm.exit' : 'you have unsaved modification,are you sure to exit?',
		'confirm.action' : 'please confirm this action?',
		'unsupported.browser' : 'unsupported browser',
		'action.denied' : 'requested action denied',
		'maximum.exceeded' : 'quantity {0} exceed maximum {1}',
		'maxsize.exceeded' : 'size {0} exceed maximum {1}',
		'max.rows.reached' : 'reached max rows : {0}',
		'pattern.coords.invalid' : 'coords should be between {0} and {1}',
		'data.invalid' : 'data invalid,please check it.',
		'repeat.not.matched' : 'repeat match failed',
		'file.too.large' : 'File too large',
		'double.check' : 'Double Check',
		'double.check.username' : 'D.C. User',
		'double.check.password' : 'D.C. Password',
		'current.password' : 'Current Password'
	},
	'zh' : {
		'ajax.loading' : '正在加载...',
		'ajax.error' : '错误,请稍后再试',
		'required' : '请填写',
		'required.only.one' : '{0}必须要有且只有一个不为空',
		'required.at.least.one' : '{0}至少要一个不为空',
		'selection.required' : '请选择',
		'email' : 'email不合法',
		'regex' : '请输入正确的格式',
		'phone' : '请填写正确的号码',
		'integer' : '请填写整数',
		'integer.positive' : '请填写正整数',
		'double' : '请填写数字',
		'double.positive' : '请填写大于零的数值',
		'save.and.create' : '保存并新建',
		'no.selection' : '没有选中',
		'no.modification' : '没有更改',
		'add' : '添加',
		'remove' : '删除',
		'browse' : '浏览文件',
		'select' : '请选择',
		'pick' : '请挑选',
		'save' : '保存',
		'restore' : '还原',
		'import' : '导入',
		'upload' : '上传',
		'snapshot' : '拍照',
		'ok' : '确定',
		'cancel' : '取消',
		'error' : '错误',
		'success' : '操作成功',
		'confirm' : '确定',
		'confirm.delete' : '确定要删除?',
		'confirm.save' : '确定要保存?',
		'confirm.exit' : '有改动未保存,确定要离开?',
		'confirm.action' : '请确认此操作',
		'true' : '是',
		'false' : '否',
		'unsupported.browser' : '你使用的浏览器不支持该功能',
		'action.denied' : '你拒绝了请求',
		'maximum.exceeded' : '数量{0}超过最大限制{1}',
		'maxsize.exceeded' : '大小{0}超过最大限制{1}',
		'max.rows.reached' : '已经达到了最大行数: {0}',
		'pattern.coords.invalid' : '坐标数必须在{0}和{1}之间',
		'data.invalid' : '数据错误, 请检查',
		'repeat.not.matched' : '两次输入不一致',
		'other' : '其他',
		'file.too.large' : '文件太大或者网络异常',
		'double.check' : '复核',
		'double.check.username' : '复核用户',
		'double.check.password' : '复核密码',
		'current.password' : '当前密码'
	},
	get : function() {
		var key = arguments[0];
		var lang = MessageBundle.lang();
		if (!MessageBundle[lang]) {
			var i = lang.indexOf('_');
			if (i > 0)
				lang = lang.substring(0, i);
			if (!MessageBundle[lang])
				lang = 'en';
		}
		var msg = MessageBundle[lang][key];
		if (typeof(msg) == 'undefined')
			msg = key;
		for (var i = 1; i < arguments.length; i++)
			msg = msg.replace('{' + (i - 1) + '}', arguments[i]);
		return msg;
	},
	lang : function() {
		var lang = $('html').attr('lang')
				|| ($.cookie('locale') || navigator.language
						|| navigator.browserLanguage || '');
		var i = lang.indexOf('-');
		if (i > 0)
			lang = lang.substring(0, i) + '_'
					+ lang.substring(i + 1).toUpperCase();
		return lang;
	},
	shortLang : function() {
		var lang = MessageBundle.lang();
		var i = lang.indexOf('_');
		if (i > 0)
			lang = lang.substring(0, i);
		return lang;
	}
};