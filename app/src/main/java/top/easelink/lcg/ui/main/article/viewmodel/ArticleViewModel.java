package top.easelink.lcg.ui.main.article.viewmodel;

import android.text.TextUtils;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import timber.log.Timber;
import top.easelink.framework.base.BaseViewModel;
import top.easelink.framework.utils.rx.SchedulerProvider;
import top.easelink.lcg.ui.main.article.view.ArticleNavigator;
import top.easelink.lcg.ui.main.model.Post;
import top.easelink.lcg.ui.main.source.remote.RxArticleService;
import top.easelink.lcg.ui.main.model.BlockException;

import java.util.ArrayList;
import java.util.List;

public class ArticleViewModel extends BaseViewModel<ArticleNavigator>
        implements ArticleAdapterListener {

    public static final int FETCH_INIT = 0;
    public static final int FETCH_MORE = 1;

    private final MutableLiveData<List<Post>> mPosts = new MutableLiveData<>();
    private final MutableLiveData<Boolean> mIsBlocked = new MutableLiveData<>();
    private final MutableLiveData<String> mArticleTitle = new MutableLiveData<>();
    private final RxArticleService articleService = RxArticleService.getInstance();
    private String mUrl;
    private String nextPageUrl;

    public ArticleViewModel(SchedulerProvider schedulerProvider) {
        super(schedulerProvider);
    }

    public void setUrl(String url) {
        this.mUrl = url;
    }

    @Override
    public void fetchArticlePost(int type) {
        setIsLoading(true);
        String requestUrl;
        if (type == FETCH_INIT) {
            requestUrl = mUrl;
        } else {
            if (TextUtils.isEmpty(nextPageUrl)) {
                // no more content
                setIsLoading(false);
                return;
            } else {
                requestUrl = nextPageUrl;
            }
        }
        getCompositeDisposable().add(articleService.getArticleDetail(requestUrl)
                .subscribeOn(getSchedulerProvider().io())
                .observeOn(getSchedulerProvider().ui())
                .subscribe(articleDetail -> {
                    String title = articleDetail.getArticleTitle();
                    if (!TextUtils.isEmpty(title)) {
                        mArticleTitle.setValue(title);
                    }
                    nextPageUrl = articleDetail.getNextPageUrl();
                    List<Post> resPostList = articleDetail.getPostList();
                    if (resPostList != null && resPostList.size()!= 0) {
                        if (type == FETCH_INIT) {
                            mPosts.setValue(resPostList);
                        } else {
                            List<Post> list = mPosts.getValue();
                            if (list != null && !list.isEmpty()) {
                                list.addAll(resPostList);
                                mPosts.setValue(list);
                            } else {
                                mPosts.setValue(resPostList);
                            }
                        }
                    }
                }, throwable -> {
                    if (throwable instanceof BlockException) {
                        mIsBlocked.setValue(true);
                        setIsLoading(false);
                    }
                    getNavigator().handleError(throwable);
                }, () -> setIsLoading(false)));
}

    public LiveData<List<Post>> getPosts() {
        return mPosts;
    }
    public LiveData<Boolean> getIsBlocked() {
        return mIsBlocked;
    }
    public LiveData<String> getArticleTitle() {
        return mArticleTitle;
    }
}