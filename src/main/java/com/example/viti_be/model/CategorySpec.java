package com.example.viti_be.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "category_specs")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class CategorySpec extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "spec_key", length = 50, nullable = false)
    private String specKey; // VD: "ram", "screen_size" - dùng làm key trong JSON

    @Column(name = "spec_name", length = 100, nullable = false)
    private String specName; // VD: "Dung lượng RAM", "Kích thước màn hình"

    @Column(name = "is_required")
    private Boolean isRequired = false;

    @Column(name = "data_type", length = 20)
    private String dataType; // TEXT, NUMBER, SELECT

    // Lưu danh sách options dưới dạng JSON String (VD: ["8GB", "16GB"])
    // Trong DB là kiểu JSONB
    @Column(columnDefinition = "jsonb")
    private String options;
}