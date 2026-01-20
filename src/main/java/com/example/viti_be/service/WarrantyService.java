package com.example.viti_be.service;

import com.example.viti_be.dto.request.*;
import com.example.viti_be.dto.response.*;
import com.example.viti_be.dto.response.pagnitation.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Service Interface cho Warranty Module
 */
public interface WarrantyService {

    // ========== TICKET CRUD ==========

    /**
     * Tạo phiếu bảo hành mới
     * @param request Thông tin phiếu
     * @param actorId Người tạo
     * @return Phiếu đã tạo
     */
    WarrantyTicketResponse createTicket(CreateWarrantyTicketRequest request, UUID actorId);

    /**
     * Cập nhật thông tin phiếu
     * @param ticketId ID phiếu
     * @param request Thông tin cập nhật
     * @param actorId Người cập nhật
     * @return Phiếu đã cập nhật
     */
    WarrantyTicketResponse updateTicket(UUID ticketId, UpdateWarrantyTicketRequest request, UUID actorId);

    /**
     * Xóa phiếu (soft delete)
     * @param ticketId ID phiếu
     * @param actorId Người xóa
     */
    void deleteTicket(UUID ticketId, UUID actorId);

    /**
     * Lấy chi tiết phiếu
     * @param ticketId ID phiếu
     * @return Chi tiết phiếu
     */
    WarrantyTicketResponse getTicketById(UUID ticketId);

    /**
     * Lấy danh sách tất cả phiếu
     * @return Danh sách phiếu (summary)
     */
    PageResponse<WarrantyTicketSummaryResponse> getAllTickets(Pageable pageable);

    /**
     * Tìm kiếm phiếu theo serial number
     * @param serialNumber Serial cần tìm
     * @return Danh sách phiếu
     */
    List<WarrantyTicketSummaryResponse> findTicketsBySerial(String serialNumber);

    /**
     * Tìm kiếm phiếu theo keyword (ticket number, serial, customer name/phone)
     * @param keyword Từ khóa
     * @return Danh sách phiếu
     */
    List<WarrantyTicketSummaryResponse> searchTickets(String keyword);

    /**
     * Lấy phiếu theo customer
     * @param customerId ID khách hàng
     * @return Danh sách phiếu
     */
    List<WarrantyTicketSummaryResponse> getTicketsByCustomer(UUID customerId);

    /**
     * Lấy phiếu theo technician
     * @param technicianId ID thợ
     * @return Danh sách phiếu
     */
    List<WarrantyTicketSummaryResponse> getTicketsByTechnician(UUID technicianId);

    /**
     * Lấy phiếu theo status
     * @param status Trạng thái
     * @return Danh sách phiếu
     */
    List<WarrantyTicketSummaryResponse> getTicketsByStatus(String status);

    // ========== STATUS MANAGEMENT ==========

    /**
     * Đổi trạng thái phiếu
     * @param ticketId ID phiếu
     * @param request Yêu cầu đổi status
     * @param actorId Người thực hiện
     * @return Phiếu đã cập nhật
     */
    WarrantyTicketResponse changeTicketStatus(UUID ticketId, ChangeTicketStatusRequest request, UUID actorId);

    /**
     * Bắt đầu sửa (RECEIVED → PROCESSING)
     * @param ticketId ID phiếu
     * @param actorId Người thực hiện
     * @return Phiếu đã cập nhật
     */
    WarrantyTicketResponse startRepair(UUID ticketId, UUID actorId);

    /**
     * Hoàn thành sửa (PROCESSING → COMPLETED)
     * @param ticketId ID phiếu
     * @param actorId Người thực hiện
     * @return Phiếu đã cập nhật
     */
    WarrantyTicketResponse completeRepair(UUID ticketId, UUID actorId);

    /**
     * Trả khách (COMPLETED → RETURNED)
     * @param ticketId ID phiếu
     * @param actorId Người thực hiện
     * @return Phiếu đã cập nhật
     */
    WarrantyTicketResponse returnToCustomer(UUID ticketId, UUID actorId);

    /**
     * Hủy phiếu
     * @param ticketId ID phiếu
     * @param reason Lý do hủy
     * @param actorId Người thực hiện
     * @return Phiếu đã hủy
     */
    WarrantyTicketResponse cancelTicket(UUID ticketId, String reason, UUID actorId);

    // ========== SERVICE & PART MANAGEMENT ==========

    /**
     * Thêm dịch vụ vào phiếu
     * @param ticketId ID phiếu
     * @param request Danh sách dịch vụ
     * @param actorId Người thực hiện
     * @return Phiếu đã cập nhật
     */
    WarrantyTicketResponse addServices(UUID ticketId, AddServicesRequest request, UUID actorId);

    /**
     * Xóa dịch vụ khỏi phiếu
     * @param ticketId ID phiếu
     * @param serviceId ID dịch vụ
     * @param actorId Người thực hiện
     * @return Phiếu đã cập nhật
     */
    WarrantyTicketResponse removeService(UUID ticketId, UUID serviceId, UUID actorId);

    /**
     * Thêm linh kiện vào phiếu
     * @param ticketId ID phiếu
     * @param request Danh sách linh kiện
     * @param actorId Người thực hiện
     * @return Phiếu đã cập nhật
     */
    WarrantyTicketResponse addParts(UUID ticketId, AddPartsRequest request, UUID actorId);

    /**
     * Xóa linh kiện khỏi phiếu
     * @param ticketId ID phiếu
     * @param partId ID linh kiện
     * @param actorId Người thực hiện
     * @return Phiếu đã cập nhật
     */
    WarrantyTicketResponse removePart(UUID ticketId, UUID partId, UUID actorId);

    // ========== TECHNICIAN MANAGEMENT ==========

    /**
     * Reassign technician
     * @param ticketId ID phiếu
     * @param request Yêu cầu reassign
     * @param actorId Admin thực hiện
     * @return Phiếu đã cập nhật
     */
    WarrantyTicketResponse reassignTechnician(UUID ticketId, ReassignTechnicianRequest request, UUID actorId);

    // ========== REPORTING ==========

    /**
     * Lấy dashboard stats
     * @return Dashboard data
     */
    WarrantyDashboardResponse getDashboard();

    /**
     * Lấy danh sách phiếu quá hạn
     * @return Danh sách phiếu
     */
    PageResponse<WarrantyTicketSummaryResponse> getOverdueTickets(Pageable pageable);

    /**
     * Lấy status history của phiếu
     * @param ticketId ID phiếu
     * @return Lịch sử đổi status
     */
    PageResponse<TicketStatusHistoryResponse> getTicketStatusHistory(UUID ticketId, Pageable pageable);
}