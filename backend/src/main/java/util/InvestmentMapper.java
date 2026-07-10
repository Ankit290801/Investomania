package com.investment.tracker.util;

import com.investment.tracker.dto.*;
import com.investment.tracker.model.*;
import org.springframework.stereotype.Component;

/**
 * Utility class for mapping between Investment entities and DTOs.
 */
@Component
public class InvestmentMapper {

    /**
     * Convert Investment entity to DTO based on type.
     */
    public InvestmentDTO toDTO(Investment investment) {
        if (investment instanceof EquityInvestment) {
            return toEquityDTO((EquityInvestment) investment);
        } else if (investment instanceof BondInvestment) {
            return toBondDTO((BondInvestment) investment);
        } else if (investment instanceof FDInvestment) {
            return toFDDTO((FDInvestment) investment);
        } else if (investment instanceof RDInvestment) {
            return toRDDTO((RDInvestment) investment);
        } else if (investment instanceof NPSInvestment) {
            return toNPSDTO((NPSInvestment) investment);
        } else if (investment instanceof PPFInvestment) {
            return toPPFDTO((PPFInvestment) investment);
        } else if (investment instanceof RealEstateInvestment) {
            return toRealEstateDTO((RealEstateInvestment) investment);
        } else if (investment instanceof CryptoInvestment) {
            return toCryptoDTO((CryptoInvestment) investment);
        } else if (investment instanceof CashInvestment) {
            return toCashDTO((CashInvestment) investment);
        }
        throw new IllegalArgumentException("Unknown investment type: " + investment.getClass());
    }

    /**
     * Convert DTO to Investment entity based on type.
     */
    public Investment toEntity(InvestmentDTO dto, Long userId) {
        if (dto instanceof EquityInvestmentDTO) {
            return toEquityEntity((EquityInvestmentDTO) dto, userId);
        } else if (dto instanceof BondInvestmentDTO) {
            return toBondEntity((BondInvestmentDTO) dto, userId);
        } else if (dto instanceof FDInvestmentDTO) {
            return toFDEntity((FDInvestmentDTO) dto, userId);
        } else if (dto instanceof RDInvestmentDTO) {
            return toRDEntity((RDInvestmentDTO) dto, userId);
        } else if (dto instanceof NPSInvestmentDTO) {
            return toNPSEntity((NPSInvestmentDTO) dto, userId);
        } else if (dto instanceof PPFInvestmentDTO) {
            return toPPFEntity((PPFInvestmentDTO) dto, userId);
        } else if (dto instanceof RealEstateInvestmentDTO) {
            return toRealEstateEntity((RealEstateInvestmentDTO) dto, userId);
        } else if (dto instanceof CryptoInvestmentDTO) {
            return toCryptoEntity((CryptoInvestmentDTO) dto, userId);
        } else if (dto instanceof CashInvestmentDTO) {
            return toCashEntity((CashInvestmentDTO) dto, userId);
        }
        throw new IllegalArgumentException("Unknown investment DTO type: " + dto.getClass());
    }

    // Equity mappings
    private EquityInvestmentDTO toEquityDTO(EquityInvestment entity) {
        return EquityInvestmentDTO.builder()
                .id(entity.getId())
                .type(entity.getType())
                .name(entity.getName())
                .currency(entity.getCurrency())
                .currentValue(entity.getCurrentValue())
                .purchaseDate(entity.getPurchaseDate())
                .symbol(entity.getSymbol())
                .quantity(entity.getQuantity())
                .avgPrice(entity.getAvgPrice())
                .market(entity.getMarket())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private EquityInvestment toEquityEntity(EquityInvestmentDTO dto, Long userId) {
        return EquityInvestment.builder()
                .id(dto.getId())
                .userId(userId)
                .type(InvestmentType.EQUITY)
                .name(dto.getName())
                .currency(dto.getCurrency())
                .currentValue(dto.getCurrentValue())
                .purchaseDate(dto.getPurchaseDate())
                .symbol(dto.getSymbol())
                .quantity(dto.getQuantity())
                .avgPrice(dto.getAvgPrice())
                .market(dto.getMarket())
                .build();
    }

    // Bond mappings
    private BondInvestmentDTO toBondDTO(BondInvestment entity) {
        return BondInvestmentDTO.builder()
                .id(entity.getId())
                .type(entity.getType())
                .name(entity.getName())
                .currency(entity.getCurrency())
                .currentValue(entity.getCurrentValue())
                .purchaseDate(entity.getPurchaseDate())
                .issuer(entity.getIssuer())
                .faceValue(entity.getFaceValue())
                .couponRate(entity.getCouponRate())
                .maturityDate(entity.getMaturityDate())
                .creditRating(entity.getCreditRating())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private BondInvestment toBondEntity(BondInvestmentDTO dto, Long userId) {
        return BondInvestment.builder()
                .id(dto.getId())
                .userId(userId)
                .type(InvestmentType.BOND)
                .name(dto.getName())
                .currency(dto.getCurrency())
                .currentValue(dto.getCurrentValue())
                .purchaseDate(dto.getPurchaseDate())
                .issuer(dto.getIssuer())
                .faceValue(dto.getFaceValue())
                .couponRate(dto.getCouponRate())
                .maturityDate(dto.getMaturityDate())
                .creditRating(dto.getCreditRating())
                .build();
    }

    // FD mappings
    private FDInvestmentDTO toFDDTO(FDInvestment entity) {
        return FDInvestmentDTO.builder()
                .id(entity.getId())
                .type(entity.getType())
                .name(entity.getName())
                .currency(entity.getCurrency())
                .currentValue(entity.getCurrentValue())
                .purchaseDate(entity.getPurchaseDate())
                .bankName(entity.getBankName())
                .principal(entity.getPrincipal())
                .interestRate(entity.getInterestRate())
                .tenureMonths(entity.getTenureMonths())
                .maturityDate(entity.getMaturityDate())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private FDInvestment toFDEntity(FDInvestmentDTO dto, Long userId) {
        return FDInvestment.builder()
                .id(dto.getId())
                .userId(userId)
                .type(InvestmentType.FD)
                .name(dto.getName())
                .currency(dto.getCurrency())
                .currentValue(dto.getCurrentValue())
                .purchaseDate(dto.getPurchaseDate())
                .bankName(dto.getBankName())
                .principal(dto.getPrincipal())
                .interestRate(dto.getInterestRate())
                .tenureMonths(dto.getTenureMonths())
                .maturityDate(dto.getMaturityDate())
                .build();
    }

    // RD mappings
    private RDInvestmentDTO toRDDTO(RDInvestment entity) {
        return RDInvestmentDTO.builder()
                .id(entity.getId())
                .type(entity.getType())
                .name(entity.getName())
                .currency(entity.getCurrency())
                .currentValue(entity.getCurrentValue())
                .purchaseDate(entity.getPurchaseDate())
                .bankName(entity.getBankName())
                .monthlyContribution(entity.getMonthlyContribution())
                .interestRate(entity.getInterestRate())
                .tenureMonths(entity.getTenureMonths())
                .maturityDate(entity.getMaturityDate())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private RDInvestment toRDEntity(RDInvestmentDTO dto, Long userId) {
        return RDInvestment.builder()
                .id(dto.getId())
                .userId(userId)
                .type(InvestmentType.RD)
                .name(dto.getName())
                .currency(dto.getCurrency())
                .currentValue(dto.getCurrentValue())
                .purchaseDate(dto.getPurchaseDate())
                .bankName(dto.getBankName())
                .monthlyContribution(dto.getMonthlyContribution())
                .interestRate(dto.getInterestRate())
                .tenureMonths(dto.getTenureMonths())
                .maturityDate(dto.getMaturityDate())
                .build();
    }

    // NPS mappings
    private NPSInvestmentDTO toNPSDTO(NPSInvestment entity) {
        return NPSInvestmentDTO.builder()
                .id(entity.getId())
                .type(entity.getType())
                .name(entity.getName())
                .currency(entity.getCurrency())
                .currentValue(entity.getCurrentValue())
                .purchaseDate(entity.getPurchaseDate())
                .accountNumber(entity.getAccountNumber())
                .contributionFrequency(entity.getContributionFrequency())
                .totalContributed(entity.getTotalContributed())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private NPSInvestment toNPSEntity(NPSInvestmentDTO dto, Long userId) {
        return NPSInvestment.builder()
                .id(dto.getId())
                .userId(userId)
                .type(InvestmentType.NPS)
                .name(dto.getName())
                .currency(dto.getCurrency())
                .currentValue(dto.getCurrentValue())
                .purchaseDate(dto.getPurchaseDate())
                .accountNumber(dto.getAccountNumber())
                .contributionFrequency(dto.getContributionFrequency())
                .totalContributed(dto.getTotalContributed())
                .build();
    }

    // PPF mappings
    private PPFInvestmentDTO toPPFDTO(PPFInvestment entity) {
        return PPFInvestmentDTO.builder()
                .id(entity.getId())
                .type(entity.getType())
                .name(entity.getName())
                .currency(entity.getCurrency())
                .currentValue(entity.getCurrentValue())
                .purchaseDate(entity.getPurchaseDate())
                .accountNumber(entity.getAccountNumber())
                .contributionFrequency(entity.getContributionFrequency())
                .totalContributed(entity.getTotalContributed())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private PPFInvestment toPPFEntity(PPFInvestmentDTO dto, Long userId) {
        return PPFInvestment.builder()
                .id(dto.getId())
                .userId(userId)
                .type(InvestmentType.PPF)
                .name(dto.getName())
                .currency(dto.getCurrency())
                .currentValue(dto.getCurrentValue())
                .purchaseDate(dto.getPurchaseDate())
                .accountNumber(dto.getAccountNumber())
                .contributionFrequency(dto.getContributionFrequency())
                .totalContributed(dto.getTotalContributed())
                .build();
    }

    // Real Estate mappings
    private RealEstateInvestmentDTO toRealEstateDTO(RealEstateInvestment entity) {
        return RealEstateInvestmentDTO.builder()
                .id(entity.getId())
                .type(entity.getType())
                .name(entity.getName())
                .currency(entity.getCurrency())
                .currentValue(entity.getCurrentValue())
                .purchaseDate(entity.getPurchaseDate())
                .propertyType(entity.getPropertyType())
                .location(entity.getLocation())
                .purchasePrice(entity.getPurchasePrice())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private RealEstateInvestment toRealEstateEntity(RealEstateInvestmentDTO dto, Long userId) {
        return RealEstateInvestment.builder()
                .id(dto.getId())
                .userId(userId)
                .type(InvestmentType.REAL_ESTATE)
                .name(dto.getName())
                .currency(dto.getCurrency())
                .currentValue(dto.getCurrentValue())
                .purchaseDate(dto.getPurchaseDate())
                .propertyType(dto.getPropertyType())
                .location(dto.getLocation())
                .purchasePrice(dto.getPurchasePrice())
                .build();
    }

    // Crypto mappings
    private CryptoInvestmentDTO toCryptoDTO(CryptoInvestment entity) {
        return CryptoInvestmentDTO.builder()
                .id(entity.getId())
                .type(entity.getType())
                .name(entity.getName())
                .currency(entity.getCurrency())
                .currentValue(entity.getCurrentValue())
                .purchaseDate(entity.getPurchaseDate())
                .symbol(entity.getSymbol())
                .quantity(entity.getQuantity())
                .avgPrice(entity.getAvgPrice())
                .exchange(entity.getExchange())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private CryptoInvestment toCryptoEntity(CryptoInvestmentDTO dto, Long userId) {
        return CryptoInvestment.builder()
                .id(dto.getId())
                .userId(userId)
                .type(InvestmentType.CRYPTO)
                .name(dto.getName())
                .currency(dto.getCurrency())
                .currentValue(dto.getCurrentValue())
                .purchaseDate(dto.getPurchaseDate())
                .symbol(dto.getSymbol())
                .quantity(dto.getQuantity())
                .avgPrice(dto.getAvgPrice())
                .exchange(dto.getExchange())
                .build();
    }

    // Cash mappings
    private CashInvestmentDTO toCashDTO(CashInvestment entity) {
        return CashInvestmentDTO.builder()
                .id(entity.getId())
                .type(entity.getType())
                .name(entity.getName())
                .currency(entity.getCurrency())
                .currentValue(entity.getCurrentValue())
                .purchaseDate(entity.getPurchaseDate())
                .bankName(entity.getBankName())
                .accountType(entity.getAccountType())
                .accountNumber(entity.getAccountNumber())
                .ifscCode(entity.getIfscCode())
                .routingNumber(entity.getRoutingNumber())
                .interestRate(entity.getInterestRate())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private CashInvestment toCashEntity(CashInvestmentDTO dto, Long userId) {
        return CashInvestment.builder()
                .id(dto.getId())
                .userId(userId)
                .type(InvestmentType.CASH)
                .name(dto.getName())
                .currency(dto.getCurrency())
                .currentValue(dto.getCurrentValue())
                .purchaseDate(dto.getPurchaseDate())
                .bankName(dto.getBankName())
                .accountType(dto.getAccountType())
                .accountNumber(dto.getAccountNumber())
                .ifscCode(dto.getIfscCode())
                .routingNumber(dto.getRoutingNumber())
                .interestRate(dto.getInterestRate())
                .notes(dto.getNotes())
                .build();
    }
}
