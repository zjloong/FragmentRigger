package com.jkb.fragment.rigger.rigger;

import static com.jkb.fragment.rigger.utils.RiggerConsts.METHOD_GET_CONTAINERVIEWID;
import static com.jkb.fragment.rigger.utils.RiggerConsts.METHOD_ON_RIGGER_BACKPRESSED;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import com.jkb.fragment.rigger.annotation.Puppet;
import com.jkb.fragment.rigger.exception.AlreadyExistException;
import com.jkb.fragment.rigger.exception.NotExistException;
import com.jkb.fragment.rigger.exception.RiggerException;
import com.jkb.fragment.rigger.exception.UnSupportException;
import com.jkb.fragment.rigger.helper.FragmentStackManager;
import com.jkb.fragment.rigger.utils.Logger;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Rigger.Used to repeat different Rigger(Strategy pattern)
 *
 * @author JingYeoh
 *         <a href="mailto:yangjing9611@foxmail.com">Email me</a>
 *         <a href="https://github.com/justkiddingbaby">Github</a>
 *         <a href="http://blog.justkiddingbaby.com">Blog</a>
 * @since Nov 20,2017
 */

@SuppressWarnings("unchecked")
abstract class _Rigger implements IRigger {

  static final String BUNDLE_KEY_FOR_RESULT = "/bundle/key/for/result";

  static _Rigger create(@NonNull Object object) {
    if (object instanceof AppCompatActivity) {
      return new _ActivityRigger((AppCompatActivity) object);
    } else if (object instanceof Fragment) {
      return new _FragmentRigger((Fragment) object);
    } else {
      throw new RiggerException(
          "Puppet Annotation class can only used on android.app.Activity or android.support.v4.app.Fragment");
    }
  }

  private Object mPuppetTarget;
  Context mContext;
  //data
  @IdRes
  private int mContainerViewId;
  private boolean mBindContainerView;
  RiggerTransaction mRiggerTransaction;
  FragmentStackManager mStackManager;

  _Rigger(Object puppetTarget) {
    this.mPuppetTarget = puppetTarget;
    //init containerViewId
    Class<?> clazz = mPuppetTarget.getClass();
    Puppet puppet = clazz.getAnnotation(Puppet.class);
    mBindContainerView = puppet.bondContainerView();
    mContainerViewId = puppet.containerViewId();
    if (mContainerViewId <= 0) {
      try {
        Method containerViewId = clazz.getMethod(METHOD_GET_CONTAINERVIEWID);
        mContainerViewId = (int) containerViewId.invoke(mPuppetTarget);
      } catch (Exception ignored) {
      }
    }
    //init helper
    mStackManager = new FragmentStackManager();
  }

  /**
   * Called when a fragment is first attached to its context.
   * {@link #onCreate(Bundle)} will be called after this.
   */
  void onAttach(Context context) {
  }

  /**
   * Called when the activity is starting.This is where most initialization should go.
   *
   * @param savedInstanceState If the activity/fragment is being re-created from
   *                           a previous saved state, this is the state.
   */
  void onCreate(Bundle savedInstanceState) {
    if (savedInstanceState != null) {
      mStackManager = FragmentStackManager.restoreStack(savedInstanceState);
    }
  }

  /**
   * Called to have the fragment instantiate its user interface view.
   *
   * @param inflater           The LayoutInflater object that can be used to inflate
   *                           any views in the fragment,
   * @param container          If non-null, this is the parent view that the fragment's
   *                           UI should be attached to.  The fragment should not add the view itself,
   *                           but this can be used to generate the LayoutParams of the view.
   * @param savedInstanceState If non-null, this fragment is being re-constructed
   *                           from a previous saved state as given here.
   *
   * @return Return the View for the fragment's UI, or null.
   */
  View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return null;
  }

  /**
   * Called immediately after {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}
   * has returned, but before any saved state has been restored in to the view.
   *
   * @param view               The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
   * @param savedInstanceState If non-null, this fragment is being re-constructed
   *                           from a previous saved state as given here.
   */
  void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
  }

  /**
   * This is the fragment-orientated version of {@link #onResume()} that you
   * can override to perform operations in the Activity at the same point
   * where its fragments are resumed.  Be sure to always call through to
   * the super-class.
   */
  void onResumeFragments() {
  }

  /**
   * Called after {@link Activity#onRestoreInstanceState}, {@link Activity#onRestart}, or
   * {@link #onPause}, for your activity to start interacting with the user.
   * This is a good place to begin mAnimations
   */
  abstract void onResume();

  /**
   * Called as part of the activity lifecycle when an activity is going into
   * the background, but has not (yet) been killed.
   */
  void onPause() {
  }

  /**
   * Called to retrieve per-instance state from an activity before being killed
   * so that the state can be restored in {@link #onCreate}
   *
   * @param outState Bundle in which to place your saved state.
   */
  abstract void onSaveInstanceState(Bundle outState);

  /**
   * Perform any final cleanup before an activity is destroyed.
   */
  abstract void onDestroy();

  /**
   * Set a hint to the system about whether this fragment's UI is currently visible
   * to the user. This hint defaults to true and is persistent across fragment instance
   * state save and restore.
   *
   * @param isVisibleToUser true if this fragment's UI is currently visible to the user (default),
   *                        false if it is not.
   */
  void setUserVisibleHint(boolean isVisibleToUser) {
  }

  /**
   * If the puppet contain onRiggerBackPressed method, then intercept the {@link #onBackPressed()} method.
   */
  void onRiggerBackPressed() {
    Class<?> clazz = mPuppetTarget.getClass();
    try {
      Method onBackPressed = clazz.getMethod(METHOD_ON_RIGGER_BACKPRESSED);
      onBackPressed.invoke(mPuppetTarget);
    } catch (Exception e) {
      onBackPressed();
    }
  }

  @Override
  public void onBackPressed() {
    String topFragmentTag = mStackManager.peek();
    //the stack is empty,close the Activity.
    if (TextUtils.isEmpty(topFragmentTag)) {
      close();
      return;
    }
    //call the top fragment's onBackPressed method.
    Fragment topFragment = mRiggerTransaction.find(topFragmentTag);
    if (topFragment == null) {
      throwException(new NotExistException(topFragmentTag));
    }
    ((_Rigger) Rigger.getRigger(topFragment)).onRiggerBackPressed();
  }

  @Override
  public Fragment findFragmentByTag(String tag) {
    if (!mStackManager.contain(tag)) return null;
    return mRiggerTransaction.find(tag);
  }

  @Override
  public void addFragment(@IdRes int containerViewId, Fragment... fragments) {
    if (fragments == null) {
      Logger.w(this, "the fragments to be added is null.");
      return;
    }
    for (Fragment fragment : fragments) {
      String fragmentTAG = Rigger.getRigger(fragment).getFragmentTAG();
      if (mStackManager.add(fragmentTAG, containerViewId)) {
        addFragmentWithAnim(fragment, containerViewId);
        mRiggerTransaction.hide(fragmentTAG);
      } else {
        throwException(new AlreadyExistException(fragmentTAG));
      }
      fragment.setUserVisibleHint(false);
    }
    mRiggerTransaction.commit();
  }

  @Override
  public void startFragment(@NonNull Fragment fragment) {
    String fragmentTAG = Rigger.getRigger(fragment).getFragmentTAG();
    if (!mStackManager.push(fragmentTAG, mContainerViewId)) {
      throwException(new AlreadyExistException(fragmentTAG));
    }
    if (getContainerViewId() <= 0) {
      throwException(new UnSupportException("ContainerViewId must be effective in class " + mPuppetTarget.getClass()));
    }
    addFragmentWithAnim(fragment, mContainerViewId);
    mRiggerTransaction.hide(getVisibleFragmentTags(getContainerViewId()));
    mRiggerTransaction.show(fragmentTAG).commit();
  }

  @Override
  public void startFragmentForResult(Object receive, @NonNull Fragment fragment, int requestCode) {
    Bundle arguments = fragment.getArguments();
    if (arguments == null) arguments = new Bundle();
    Message message = Message.obtain();
    message.obj = receive;
    message.arg1 = requestCode;
    arguments.putParcelable(BUNDLE_KEY_FOR_RESULT, message);
    fragment.setArguments(arguments);
    startFragment(fragment);
  }

  @Override
  public void startFragmentForResult(@NonNull Fragment fragment, int requestCode) {
    startFragmentForResult(null, fragment, requestCode);
  }

  @Override
  public void startPopFragment() {
    startPopFragment(null);
  }

  /**
   * show pop fragment and start animation.
   */
  void startPopFragment(Animation animation) {
    String topFragmentTag = mStackManager.peek();
    mRiggerTransaction.hide(getVisibleFragmentTags(getContainerViewId()));
    Fragment topFragment = mRiggerTransaction.find(topFragmentTag);
    if (!TextUtils.isEmpty(topFragmentTag) && topFragment != null) {
      if (animation != null && topFragment.getView() != null) {
        topFragment.getView().startAnimation(animation);
        //cancel the default animation and use the custom animation.
      }
      mRiggerTransaction.setCustomAnimations(0, 0);
      mRiggerTransaction.show(topFragmentTag);
    }
    mRiggerTransaction.commit();
  }

  @Override
  public void showFragment(@NonNull Fragment fragment, @IdRes int containerViewId) {
    String fragmentTAG = Rigger.getRigger(fragment).getFragmentTAG();
    if (mStackManager.add(fragmentTAG, containerViewId)) {
      addFragmentWithAnim(fragment, containerViewId);
    }
    String[] fragmentTags = mStackManager.getFragmentTags(containerViewId);
    for (String tag : fragmentTags) {
      Fragment hideFrag = mRiggerTransaction.find(tag);
      if (hideFrag == null) continue;
      hideFrag.setUserVisibleHint(false);
    }
    fragment.setUserVisibleHint(true);
    showFragmentWithAnim(fragment);
    mRiggerTransaction.hide(getVisibleFragmentTags(containerViewId));
    mRiggerTransaction.commit();
  }

  @Override
  public void showFragment(@NonNull String tag) {
    int containerViewId = mStackManager.getContainer(tag);
    if (containerViewId == 0) {
      throwException(new NotExistException(tag));
    }
    showFragment(mRiggerTransaction.find(tag), containerViewId);
  }

  @Override
  public void hideFragment(@NonNull Fragment fragment) {
    _FragmentRigger rigger = (_FragmentRigger) Rigger.getRigger(fragment);
    String fragmentTAG = rigger.getFragmentTAG();
    mRiggerTransaction.setCustomAnimations(rigger.mPopEnterAnim, rigger.mExitAnim);
    mRiggerTransaction.hide(fragmentTAG)
        .commit();
  }

  @Override
  public void hideFragment(@NonNull String tag) {
    if (!mStackManager.contain(tag)) {
      throwException(new NotExistException(tag));
    }
    hideFragment(mRiggerTransaction.find(tag));
  }

  @Override
  public void replaceFragment(@NonNull Fragment fragment, @IdRes int containerViewId) {
    String fragmentTAG = Rigger.getRigger(fragment).getFragmentTAG();
    addFragmentWithAnim(fragment, containerViewId);
    mRiggerTransaction.remove(mStackManager.getFragmentTags(containerViewId))
        .show(fragmentTAG)
        .commit();
    mStackManager.remove(containerViewId);
    mStackManager.add(fragmentTAG, containerViewId);
  }

  @Override
  public void close(@NonNull Fragment fragment) {
    String fragmentTAG = Rigger.getRigger(fragment).getFragmentTAG();
    if (!mStackManager.remove(fragmentTAG)) {
      throwException(new NotExistException(fragmentTAG));
    }
    //if the stack is empty and the puppet is bond container view.then close the fragment.
    if (isBondContainerView() && mStackManager.getFragmentStack().empty()) {
      close();
    } else {
      //if the puppet is not bond container,then remove the fragment onto the container.
      //and show the Fragment's content view.
      mRiggerTransaction.remove(fragmentTAG).commit();
    }
  }

  @Override
  public int getContainerViewId() {
    return mContainerViewId;
  }

  @Override
  public boolean isBondContainerView() {
    return mBindContainerView;
  }

  @Override
  final public Stack<String> getFragmentStack() {
    if (mStackManager == null || mStackManager.getFragmentStack() == null) return new Stack<>();
    //noinspection unchecked
    return (Stack<String>) mStackManager.getFragmentStack().clone();
  }

  @Override
  public void printStack() {
    StringBuilder sb = new StringBuilder();
    sb.append(getFragmentTAG());
    Stack<String> stack = mStackManager.getFragmentStack();
    printStack(sb, this, stack, 1);
    Log.i("Rigger", sb.toString());
  }

  private void printStack(StringBuilder sb, _Rigger rigger, Stack<String> stack, int level) {
    if (stack == null || stack.empty()) return;
    for (int p = stack.size() - 1; p >= 0; p--) {
      String tag = stack.get(p);
      sb.append("\n");
      sb.append("┃");
      if (level != 1) {
        for (int i = 0; i < level; i++) {
          sb.append(" ").append(" ").append(" ").append(" ");
        }
      }
      for (int i = 0; i < level; i++) {
        sb.append("\t");
      }
      Fragment fragment = rigger.mRiggerTransaction.find(tag);
      _Rigger childRigger = (_Rigger) Rigger.getRigger(fragment);
      Stack<String> childStack = childRigger.getFragmentStack();
      if (p > 0 && childStack.isEmpty()) {
        sb.append("┠");
      } else {
        sb.append("┖");
      }
      sb.append("————");
      sb.append(tag);
      printStack(sb, childRigger, childStack, level + 1);
    }
  }

  /**
   * Add a fragment and set the fragment's mAnimations
   */
  private void addFragmentWithAnim(Fragment fragment, int containerViewId) {
    _FragmentRigger rigger = (_FragmentRigger) Rigger.getRigger(fragment);
    mRiggerTransaction.setCustomAnimations(rigger.mEnterAnim, rigger.mPopExitAnim);
    mRiggerTransaction.add(containerViewId, fragment, rigger.getFragmentTAG());
  }

  /**
   * Show a fragment and set the fragment's mAnimations
   */
  private void showFragmentWithAnim(Fragment fragment) {
    _FragmentRigger rigger = (_FragmentRigger) Rigger.getRigger(fragment);
    mRiggerTransaction.setCustomAnimations(rigger.mPopEnterAnim, rigger.mExitAnim);
    mRiggerTransaction.show(rigger.getFragmentTAG());
  }

  /**
   * Throw the exception.
   */
  void throwException(RiggerException e) {
    throw e;
  }

  /**
   * Return fragments tag which the fragment's view is visible and is add onto the container view.
   *
   * @param containerViewId The container view's id to be found.
   *
   * @return The fragment tags.
   */
  private String[] getVisibleFragmentTags(@IdRes int containerViewId) {
    List<String> result = new ArrayList<>();
    String[] fragmentTags = mStackManager.getFragmentTags(containerViewId);
    //noinspection ConstantConditions
    if (fragmentTags == null) return result.toArray(new String[result.size()]);
    for (String tag : fragmentTags) {
      Fragment fragment = mRiggerTransaction.find(tag);
      if (fragment != null && !fragment.isHidden() &&
          fragment.getView() != null && fragment.getView().getVisibility() == View.VISIBLE) {
        result.add(tag);
      }
    }
    return result.toArray(new String[result.size()]);
  }
}