package com.example.viti_be.dto.response.pagnitation;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Đối tượng chứa kết quả phân trang")
public class PageResponse<T> {

    @Schema(description = "Danh sách nội dung của trang hiện tại")
    private List<T> content;

    @Schema(description = "Tổng số phần tử trên tất cả các trang")
    private long totalElements;

    @Schema(description = "Tổng số trang")
    private int totalPages;

    @Schema(description = "Số trang hiện tại (bắt đầu từ 0)")
    private int currentPage;

    @Schema(description = "Kích thước của trang")
    private int pageSize;

    @Schema(description = "Trang này có phải là trang cuối cùng không?")
    private boolean last;

    @Schema(description = "Trang này có phải là trang đầu tiên không?")
    private boolean first;

    @Schema(description = "Số lượng phần tử trên trang hiện tại")
    private int numberOfElements;

    /**
     * Hàm tiện ích static để tạo PageResponse từ Page<Entity> và Mapper.
     * Giúp code trong Service ngắn gọn chỉ còn 1 dòng.
     *
     * @param pageData Trang dữ liệu lấy từ Repository (Entity)
     * @param mapper Hàm để map từ Entity sang DTO (vd: userMapper::toResponse)
     * @param <E> Kiểu Entity
     * @param <D> Kiểu DTO
     * @return PageResponse<D>
     */
    public static <E, D> PageResponse<D> from(Page<E> pageData, Function<E, D> mapper) {
        // 1. Map List<Entity> -> List<DTO>
        List<D> dtos = pageData.getContent().stream()
                .map(mapper)
                .collect(Collectors.toList());

        // 2. Build và trả về
        return PageResponse.<D>builder()
                .content(dtos)
                .totalElements(pageData.getTotalElements())
                .totalPages(pageData.getTotalPages())
                .currentPage(pageData.getNumber())
                .pageSize(pageData.getSize())
                .last(pageData.isLast())
                .first(pageData.isFirst())
                .numberOfElements(pageData.getNumberOfElements())
                .build();
    }
}
