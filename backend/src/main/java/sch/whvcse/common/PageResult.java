package sch.whvcse.common;

import java.util.List;
import lombok.Data;

/**
 * 分页结果封装。
 * 需求文档 7 章：列表接口统一以 { list, total, page, size } 返回。
 */
@Data
public class PageResult<T> {

    /** 当前页数据 */
    private List<T> list;
    /** 总条数 */
    private long total;
    /** 当前页码（从 1 开始） */
    private int page;
    /** 每页条数 */
    private int size;

    public PageResult(List<T> list, long total, int page, int size) {
        this.list = list;
        this.total = total;
        this.page = page;
        this.size = size;
    }

    /** 内存分页辅助（数据量小的场景直接内存切页） */
    public static <T> PageResult<T> of(List<T> all, int page, int size) {
        int p = Math.max(1, page);
        int s = Math.max(1, size);
        int from = Math.min((p - 1) * s, all.size());
        int to = Math.min(from + s, all.size());
        return new PageResult<>(all.subList(from, to), all.size(), p, s);
    }
}
