package doext.implement;

import java.util.Map;
import org.json.JSONObject;
import android.annotation.SuppressLint;
import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import core.DoServiceContainer;
import core.helper.DoJsonHelper;
import core.helper.DoScriptEngineHelper;
import core.helper.DoTextHelper;
import core.helper.DoUIModuleHelper;
import core.interfaces.DoIListData;
import core.interfaces.DoIScriptEngine;
import core.interfaces.DoIUIModuleView;
import core.object.DoInvokeResult;
import core.object.DoMultitonModule;
import core.object.DoUIModule;
import doext.define.do_VerticalSlideView_IMethod;
import doext.define.do_VerticalSlideView_MAbstract;
import doext.ui.vslideview.PagerAdapter;
import doext.ui.vslideview.VerticalViewPager;

/**
 * 自定义扩展UIView组件实现类，此类必须继承相应VIEW类，并实现DoIUIModuleView,
 * do_VerticalSlideView_IMethod接口； #如何调用组件自定义事件？可以通过如下方法触发事件：
 * this.model.getEventCenter().fireEvent(_messageName, jsonResult);
 * 参数解释：@_messageName字符串事件名称，@jsonResult传递事件参数对象； 获取DoInvokeResult对象方式new
 * DoInvokeResult(this.model.getUniqueKey());
 */
@SuppressLint("ClickableViewAccessibility")
public class do_VerticalSlideView_View extends VerticalViewPager implements DoIUIModuleView, do_VerticalSlideView_IMethod {
	/**
	 * 每个UIview都会引用一个具体的model实例；
	 */
	private do_VerticalSlideView_MAbstract model;
	private MyPagerAdapter mPagerAdapter;
	private String[] templates;
	private int currentItem;
	private boolean allowGesture = true;

	public do_VerticalSlideView_View(Context context) {
		super(context);
		mPagerAdapter = new MyPagerAdapter();
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent e) {
		if (!allowGesture) {
			return false;
		}
		return super.onInterceptTouchEvent(e);
	}

	@Override
	public boolean onTouchEvent(MotionEvent e) {
		if (!allowGesture) {
			return false;
		}
		return super.onTouchEvent(e);
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		boolean ret = super.dispatchTouchEvent(ev);
		if (ret) {
			getParent().requestDisallowInterceptTouchEvent(true);
		}
		return ret;
	}

	/**
	 * 初始化加载view准备,_doUIModule是对应当前UIView的model实例
	 */
	@Override
	public void loadView(DoUIModule _doUIModule) throws Exception {
		this.model = (do_VerticalSlideView_MAbstract) _doUIModule;
		this.setOnPageChangeListener(new MyPageChangeListener());
	}

	/**
	 * 动态修改属性值时会被调用，方法返回值为true表示赋值有效，并执行onPropertiesChanged，否则不进行赋值；
	 * 
	 * @_changedValues<key,value>属性集（key名称、value值）；
	 */
	@Override
	public boolean onPropertiesChanging(Map<String, String> _changedValues) {
		if (_changedValues.containsKey("templates")) {
			String value = _changedValues.get("templates");
			if ("".equals(value)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 属性赋值成功后被调用，可以根据组件定义相关属性值修改UIView可视化操作；
	 * 
	 * @_changedValues<key,value>属性集（key名称、value值）；
	 */
	@Override
	public void onPropertiesChanged(Map<String, String> _changedValues) {
		DoUIModuleHelper.handleBasicViewProperChanged(this.model, _changedValues);
		if (_changedValues.containsKey("templates")) {
			initViewTemplate(_changedValues.get("templates"));
		}
		if (_changedValues.containsKey("index")) {
			currentItem = DoTextHelper.strToInt(_changedValues.get("index"), 0);
			this.setCurrentItem(currentItem, false);
		}
		if (_changedValues.containsKey("allowGesture")) {
			allowGesture = DoTextHelper.strToBool(_changedValues.get("allowGesture"), false);
		}
	}

	private void initViewTemplate(String data) {
		try {
			templates = data.split(",");
		} catch (Exception e) {
			DoServiceContainer.getLogEngine().writeError("解析templates错误： \t", e);
		}
	}

	/**
	 * 同步方法，JS脚本调用该组件对象方法时会被调用，可以根据_methodName调用相应的接口实现方法；
	 * 
	 * @_methodName 方法名称
	 * @_dictParas 参数（K,V）
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public boolean invokeSyncMethod(String _methodName, JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		if ("bindItems".equals(_methodName)) {
			bindItems(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		if ("refreshItems".equals(_methodName)) {
			refreshItems(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		return false;
	}

	@Override
	public void bindItems(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		String _address = DoJsonHelper.getString(_dictParas, "data", "");
		if (_address == null || _address.length() <= 0)
			throw new Exception("doVerticalSlideView 未指定 data参数！");
		DoMultitonModule _multitonModule = DoScriptEngineHelper.parseMultitonModule(_scriptEngine, _address);
		if (_multitonModule == null)
			throw new Exception("doVerticalSlideView data参数无效！");
		if (_multitonModule instanceof DoIListData) {
			DoIListData _data = (DoIListData) _multitonModule;
			setItems(_data);
		}
	}

	@Override
	public void refreshItems(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		this.getAdapter().notifyDataSetChanged();
		this.setCurrentItem(currentItem);
	}

	/**
	 * 异步方法（通常都处理些耗时操作，避免UI线程阻塞），JS脚本调用该组件对象方法时会被调用， 可以根据_methodName调用相应的接口实现方法；
	 * 
	 * @_methodName 方法名称
	 * @_dictParas 参数（K,V）
	 * @_scriptEngine 当前page JS上下文环境
	 * @_callbackFuncName 回调函数名 #如何执行异步方法回调？可以通过如下方法：
	 *                    _scriptEngine.callback(_callbackFuncName,
	 *                    _invokeResult);
	 *                    参数解释：@_callbackFuncName回调函数名，@_invokeResult传递回调函数参数对象；
	 *                    获取DoInvokeResult对象方式new
	 *                    DoInvokeResult(this.getUniqueKey());
	 */
	@Override
	public boolean invokeAsyncMethod(String _methodName, JSONObject _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) {
		//...do something
		return false;
	}

	/**
	 * 释放资源处理，前端JS脚本调用closePage或执行removeui时会被调用；
	 */
	@Override
	public void onDispose() {
		//...do something
	}

	/**
	 * 重绘组件，构造组件时由系统框架自动调用；
	 * 或者由前端JS脚本调用组件onRedraw方法时被调用（注：通常是需要动态改变组件（X、Y、Width、Height）属性时手动调用）
	 */
	@Override
	public void onRedraw() {
		this.setLayoutParams(DoUIModuleHelper.getLayoutParams(this.model));
	}

	class MyPagerAdapter extends PagerAdapter {

		private DoIListData listData;

		public void bindData(DoIListData _listData) {
			this.listData = _listData;
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			container.removeView((View) object);
			((DoIUIModuleView) object).getModel().dispose();
		}

		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			View view = null;
			try {
				JSONObject childData = (JSONObject) ((DoIListData) listData).getData(position);
				int _index = DoTextHelper.strToInt(DoJsonHelper.getString(childData, "template", "0"), -1);
				if (null == templates || templates.length == 0) {
					throw new RuntimeException("do_VertivalSlideView模板templates为空！");
				}
				String templatePath = templates[_index];
				if (templatePath == null) {
					throw new RuntimeException("绑定一个无效的模版Index值！");
				}
				DoUIModule uiModule = DoServiceContainer.getUIModuleFactory().createUIModuleBySourceFile(templatePath, model.getCurrentPage(), true);
				DoIUIModuleView _doIUIModuleView = uiModule.getCurrentUIModuleView();
				_doIUIModuleView.getModel().setModelData(childData);
				view = (View) _doIUIModuleView;
				container.addView(view);
			} catch (Exception e) {
				DoServiceContainer.getLogEngine().writeError("解析data数据错误： \t", e);
			}
			return view;
		}

		@Override
		public int getCount() {
			if (listData == null) {
				return 0;
			}
			return listData.getCount();
		}

		@Override
		public boolean isViewFromObject(View arg0, Object arg1) {
			return arg0 == arg1;
		}

		@Override
		public void notifyDataSetChanged() {
			try {
				if (listData == null) {
					return;
				}
			} catch (Exception e) {
				DoServiceContainer.getLogEngine().writeError("do_VerticalSlideView_View bindData\n\t ", e);
			}
			super.notifyDataSetChanged();
		}

	}

	class MyPageChangeListener implements VerticalViewPager.OnPageChangeListener {

		@Override
		public void onPageScrollStateChanged(int arg0) {

		}

		@Override
		public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
			if (positionOffset == 0.0 && positionOffsetPixels == 0) {
				getParent().requestDisallowInterceptTouchEvent(false);
			}
		}

		@Override
		public void onPageSelected(int position) {
			DoInvokeResult invokeResult = new DoInvokeResult(model.getUniqueKey());
			invokeResult.setResultInteger(position);
			try {
				model.setPropertyValue("index", position + "");
			} catch (Exception e) {
				e.printStackTrace();
			}
			model.getEventCenter().fireEvent("indexChanged", invokeResult);
		}
	}

	public void setItems(Object _obj) {
		if (_obj == null)
			return;
		if (_obj instanceof DoIListData) {
			DoIListData _listData = (DoIListData) _obj;
			mPagerAdapter.bindData(_listData);
			this.setAdapter(mPagerAdapter);
			this.setCurrentItem(currentItem);
		}
	}

	/**
	 * 获取当前model实例
	 */
	@Override
	public DoUIModule getModel() {
		return model;
	}

}