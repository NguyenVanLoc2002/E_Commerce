package com.locnguyen.ecommerce.domains.inventory.enums;

/**
 * Stock movement types — each maps to a specific inventory operation.
 *
 * @see com.locnguyen.ecommerce.domains.inventory.lifecycle.InventoryLifecycle
 */
public enum StockMovementType {
    /** Nhập kho hàng mới (tăng on_hand) */
    IMPORT,
    /** Xuất kho hàng (giảm on_hand) */
    EXPORT,
    /** Giữ hàng tạm khi checkout */
    RESERVE,
    /** Nhả hàng đã giữ khi hủy đơn */
    RELEASE,
    /** Điều chỉnh kho thủ công */
    ADJUST,
    /** Nhận lại hàng đã bán bị trả */
    RETURN
}
