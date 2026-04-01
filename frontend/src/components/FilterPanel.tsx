import type { Filters } from '../types/filters';
import type { NewsType } from '../types/news';
import { useMemo, useState } from 'react';
import Box from '@mui/material/Box';
import FormControl from '@mui/material/FormControl';
import InputLabel from '@mui/material/InputLabel';
import MenuItem from '@mui/material/MenuItem';
import Select, { type SelectChangeEvent } from '@mui/material/Select';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import dayjs from 'dayjs';

export interface FilterState {
  type?: NewsType | '';
  district?: string;
  startDate?: string;
  endDate?: string;
  search?: string;
}

interface FilterPanelProps {
  filters: Filters | null;
  value: FilterState;
  onChange: (next: FilterState) => void;
  onSubmit: () => void;
}

const typeLabels: Record<NewsType, string> = {
  TRAFIK_KAZASI: 'Trafik kazası',
  YANGIN: 'Yangın',
  ELEKTRIK_KESINTISI: 'Elektrik kesintisi',
  HIRSIZLIK: 'Hırsızlık / güvenlik',
  KULTUREL_ETKINLIK: 'Kültürel etkinlik',
  DIGER: 'Diğer',
};

const typeDotColors: Record<NewsType, string> = {
  TRAFIK_KAZASI: '#dc2626',
  YANGIN: '#ea580c',
  ELEKTRIK_KESINTISI: '#ca8a04',
  HIRSIZLIK: '#7c3aed',
  KULTUREL_ETKINLIK: '#16a34a',
  DIGER: '#6b7280',
};

const menuPaperSx = {
  mt: 1,
  borderRadius: '14px',
  border: '1px solid rgba(148, 163, 184, 0.22)',
  background: 'linear-gradient(180deg, rgba(30, 41, 59, 0.98) 0%, rgba(15, 23, 42, 0.99) 100%)',
  backdropFilter: 'blur(16px)',
  WebkitBackdropFilter: 'blur(16px)',
  maxHeight: 360,
  boxShadow:
    '0 20px 50px rgba(0, 0, 0, 0.55), 0 0 0 1px rgba(255, 255, 255, 0.04)',
};

const selectSx = {
  borderRadius: '12px',
  color: '#f1f5f9',
  backgroundColor: 'rgba(0, 0, 0, 0.28)',
  transition: 'background-color 0.2s ease, box-shadow 0.2s ease',
  '&:hover': {
    backgroundColor: 'rgba(0, 0, 0, 0.38)',
  },
  '& .MuiOutlinedInput-notchedOutline': {
    borderColor: 'rgba(255, 255, 255, 0.1)',
  },
  '&:hover .MuiOutlinedInput-notchedOutline': {
    borderColor: 'rgba(16, 185, 129, 0.4)',
  },
  '&.Mui-focused': {
    backgroundColor: 'rgba(0, 0, 0, 0.45)',
  },
  '&.Mui-focused .MuiOutlinedInput-notchedOutline': {
    borderColor: '#10b981',
    borderWidth: '1px',
  },
  '& .MuiSvgIcon-root': {
    color: '#94a3b8',
  },
};

const menuItemSx = {
  borderRadius: '10px',
  mx: 0.75,
  my: 0.2,
  py: 1.15,
  px: 1.25,
  minHeight: 44,
  color: '#e2e8f0',
  fontSize: '0.9rem',
  fontWeight: 500,
  '&:hover': {
    backgroundColor: 'rgba(16, 185, 129, 0.1)',
  },
  '&.Mui-selected': {
    backgroundColor: 'rgba(16, 185, 129, 0.2) !important',
  },
  '&.Mui-selected:hover': {
    backgroundColor: 'rgba(16, 185, 129, 0.28) !important',
  },
};

const menuItemMutedSx = {
  ...menuItemSx,
  fontStyle: 'italic',
  color: '#94a3b8',
  fontWeight: 400,
};

const labelSx = {
  color: '#94a3b8',
  fontSize: '0.875rem',
  fontWeight: 500,
  '&.Mui-focused': {
    color: '#6ee7b7',
  },
  '&.MuiInputLabel-shrink': {
    color: '#64748b',
  },
};

/** DatePicker içindeki TextField — Select ile aynı çerçeve */
const dateFieldSx = {
  flex: 1,
  minWidth: 0,
  width: '100%',
  '& .MuiOutlinedInput-root': {
    borderRadius: '12px',
    color: '#f1f5f9',
    backgroundColor: 'rgba(0, 0, 0, 0.28)',
    transition: 'background-color 0.2s ease, box-shadow 0.2s ease',
    '&:hover': {
      backgroundColor: 'rgba(0, 0, 0, 0.38)',
    },
    '& .MuiOutlinedInput-notchedOutline': {
      borderColor: 'rgba(255, 255, 255, 0.1)',
    },
    '&:hover .MuiOutlinedInput-notchedOutline': {
      borderColor: 'rgba(16, 185, 129, 0.4)',
    },
    '&.Mui-focused': {
      backgroundColor: 'rgba(0, 0, 0, 0.45)',
    },
    '&.Mui-focused .MuiOutlinedInput-notchedOutline': {
      borderColor: '#10b981',
      borderWidth: '1px',
    },
  },
  '& .MuiOutlinedInput-input': {
    py: 1.15,
    fontSize: '0.9rem',
    fontVariantNumeric: 'tabular-nums',
    color: '#f1f5f9',
  },
  '& .MuiIconButton-root': {
    color: '#94a3b8',
    borderRadius: '10px',
    '&:hover': {
      bgcolor: 'rgba(16, 185, 129, 0.12)',
      color: '#6ee7b7',
    },
  },
};

/** Açılır takvim paneli — native beyaz popup yerine koyu tema */
const pickerPopperSx = {
  '& .MuiPaper-root': {
    mt: 0.5,
    background: 'linear-gradient(165deg, rgba(30, 41, 59, 0.98) 0%, rgba(15, 23, 42, 0.99) 100%)',
    border: '1px solid rgba(148, 163, 184, 0.28)',
    borderRadius: '16px',
    boxShadow: '0 24px 56px rgba(0, 0, 0, 0.6), 0 0 0 1px rgba(255, 255, 255, 0.04)',
    color: '#e2e8f0',
    overflow: 'hidden',
    backdropFilter: 'blur(16px)',
  },
};

const pickerLayoutSx = {
  '& .MuiPickersLayout-root': {
    bgcolor: 'transparent',
  },
  '& .MuiPickersLayout-actionBar': {
    borderTop: '1px solid rgba(148, 163, 184, 0.18)',
    px: 1.25,
    py: 1,
    bgcolor: 'rgba(0, 0, 0, 0.25)',
    justifyContent: 'flex-end',
    gap: 0.5,
    '& .MuiButton-root': {
      color: '#6ee7b7',
      fontWeight: 600,
      fontSize: '0.82rem',
      textTransform: 'none',
      borderRadius: '10px',
      px: 1.5,
      '&:hover': {
        bgcolor: 'rgba(16, 185, 129, 0.14)',
      },
    },
  },
  '& .MuiPickersCalendarHeader-root': {
    color: '#f8fafc',
    pl: 1,
    pr: 0.5,
    pt: 0.75,
    pb: 0.25,
    '& .MuiIconButton-root': {
      color: '#94a3b8',
      '&:hover': {
        bgcolor: 'rgba(255, 255, 255, 0.08)',
        color: '#e2e8f0',
      },
    },
    '& .MuiPickersCalendarHeader-label': {
      fontWeight: 700,
      fontSize: '0.95rem',
      letterSpacing: '0.02em',
    },
  },
  '& .MuiPickersDay-root': {
    color: '#cbd5e1',
    fontWeight: 500,
    borderRadius: '10px',
    '&:hover': {
      bgcolor: 'rgba(16, 185, 129, 0.18)',
    },
    '&.Mui-selected': {
      bgcolor: '#10b981 !important',
      color: '#fff !important',
      fontWeight: 700,
      '&:hover': {
        bgcolor: '#059669 !important',
      },
      '&:focus': {
        bgcolor: '#059669 !important',
      },
    },
    '&.MuiPickersDay-today': {
      border: '1px solid rgba(16, 185, 129, 0.65) !important',
      fontWeight: 700,
    },
  },
  '& .MuiDayCalendar-weekDayLabel': {
    color: '#64748b',
    fontWeight: 700,
    fontSize: '0.7rem',
  },
};

export function FilterPanel({ filters, value, onChange, onSubmit }: FilterPanelProps) {
  const [localState, setLocalState] = useState<FilterState>(value);

  const handleFieldChange = <K extends keyof FilterState>(key: K, val: FilterState[K]) => {
    const next = { ...localState, [key]: val };
    setLocalState(next);
    onChange(next);
  };

  const districts = useMemo(
    () => (filters?.districts ?? []).filter((d) => d && d.trim().length > 0),
    [filters],
  );

  const types = filters?.types ?? [];

  const menuProps = {
    PaperProps: { sx: menuPaperSx },
    MenuListProps: {
      sx: { py: 0.75 },
    },
    anchorOrigin: { vertical: 'bottom' as const, horizontal: 'left' as const },
    transformOrigin: { vertical: 'top' as const, horizontal: 'left' as const },
  };

  return (
    <div className="filter-panel-inner">
      <FormControl fullWidth className="filter-mui-field" size="small">
        <InputLabel id="filter-type-label" sx={labelSx} shrink>
          Haber türü
        </InputLabel>
        <Select
          labelId="filter-type-label"
          id="type"
          label="Haber türü"
          notched
          value={localState.type ?? ''}
          displayEmpty
          onChange={(e: SelectChangeEvent) =>
            handleFieldChange('type', e.target.value as NewsType | '')
          }
          MenuProps={menuProps}
          sx={{
            ...selectSx,
            '& .MuiSelect-select': { py: 1.15, display: 'flex', alignItems: 'center' },
          }}
          renderValue={(selected) => {
            if (!selected) {
              return (
                <Box component="span" sx={{ color: '#94a3b8', fontWeight: 500 }}>
                  Tüm türler
                </Box>
              );
            }
            const t = selected as NewsType;
            return (
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.25 }}>
                <Box
                  component="span"
                  sx={{
                    width: 9,
                    height: 9,
                    borderRadius: '50%',
                    bgcolor: typeDotColors[t],
                    flexShrink: 0,
                    boxShadow: `0 0 0 2px rgba(0,0,0,0.35), 0 0 12px ${typeDotColors[t]}55`,
                  }}
                />
                {typeLabels[t] ?? t}
              </Box>
            );
          }}
        >
          <MenuItem value="" sx={menuItemMutedSx}>
            Tüm türler
          </MenuItem>
          {types.map((t) => (
            <MenuItem key={t} value={t} sx={menuItemSx}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, width: '100%' }}>
                <Box
                  component="span"
                  sx={{
                    width: 10,
                    height: 10,
                    borderRadius: '50%',
                    bgcolor: typeDotColors[t],
                    flexShrink: 0,
                    boxShadow: `0 0 0 2px rgba(0,0,0,0.25)`,
                  }}
                />
                <span>{typeLabels[t] ?? t}</span>
              </Box>
            </MenuItem>
          ))}
        </Select>
      </FormControl>

      <FormControl fullWidth className="filter-mui-field" size="small">
        <InputLabel id="filter-district-label" sx={labelSx} shrink>
          İlçe
        </InputLabel>
        <Select
          labelId="filter-district-label"
          id="district"
          label="İlçe"
          notched
          value={localState.district ?? ''}
          displayEmpty
          onChange={(e: SelectChangeEvent) =>
            handleFieldChange('district', e.target.value || undefined)
          }
          MenuProps={menuProps}
          sx={{
            ...selectSx,
            '& .MuiSelect-select': { py: 1.15, display: 'flex', alignItems: 'center' },
          }}
          renderValue={(selected) => {
            if (!selected) {
              return (
                <Box component="span" sx={{ color: '#94a3b8', fontWeight: 500 }}>
                  Tüm ilçeler
                </Box>
              );
            }
            return (
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.25 }}>
                <Box
                  component="span"
                  aria-hidden
                  sx={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    width: 22,
                    height: 22,
                    borderRadius: '8px',
                    bgcolor: 'rgba(56, 189, 248, 0.12)',
                    border: '1px solid rgba(56, 189, 248, 0.25)',
                    color: '#7dd3fc',
                    fontSize: '0.75rem',
                  }}
                >
                  ◉
                </Box>
                {selected}
              </Box>
            );
          }}
        >
          <MenuItem value="" sx={menuItemMutedSx}>
            Tüm ilçeler
          </MenuItem>
          {districts.map((d) => (
            <MenuItem key={d} value={d} sx={menuItemSx}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, width: '100%' }}>
                <Box
                  component="span"
                  sx={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    width: 28,
                    height: 28,
                    borderRadius: '9px',
                    bgcolor: 'rgba(56, 189, 248, 0.1)',
                    border: '1px solid rgba(56, 189, 248, 0.22)',
                    color: '#7dd3fc',
                    fontSize: '0.7rem',
                    fontWeight: 700,
                    flexShrink: 0,
                  }}
                >
                  {d.slice(0, 2).toUpperCase()}
                </Box>
                <span>{d}</span>
              </Box>
            </MenuItem>
          ))}
        </Select>
      </FormControl>

      <Box sx={{ width: '100%' }}>
        <Box
          component="p"
          sx={{
            m: 0,
            mb: 0.85,
            fontSize: '0.72rem',
            fontWeight: 700,
            letterSpacing: '0.12em',
            textTransform: 'uppercase',
            color: '#64748b',
          }}
        >
          Tarih aralığı
        </Box>
        <Box
          sx={{
            display: 'flex',
            flexDirection: 'row',
            gap: 1.5,
            width: '100%',
            alignItems: 'stretch',
          }}
        >
          <DatePicker
            format="DD.MM.YYYY"
            label="Başlangıç"
            value={localState.startDate ? dayjs(localState.startDate) : null}
            onChange={(v) =>
              handleFieldChange(
                'startDate',
                v?.isValid() ? v.format('YYYY-MM-DD') : undefined,
              )
            }
            slotProps={{
              popper: { sx: pickerPopperSx },
              layout: { sx: pickerLayoutSx },
              actionBar: { actions: ['clear', 'today'] },
              textField: {
                id: 'startDate',
                size: 'small',
                fullWidth: true,
                InputLabelProps: { shrink: true, sx: labelSx },
                sx: dateFieldSx,
              },
            }}
          />
          <DatePicker
            format="DD.MM.YYYY"
            label="Bitiş"
            value={localState.endDate ? dayjs(localState.endDate) : null}
            onChange={(v) =>
              handleFieldChange('endDate', v?.isValid() ? v.format('YYYY-MM-DD') : undefined)
            }
            slotProps={{
              popper: { sx: pickerPopperSx },
              layout: { sx: pickerLayoutSx },
              actionBar: { actions: ['clear', 'today'] },
              textField: {
                id: 'endDate',
                size: 'small',
                fullWidth: true,
                InputLabelProps: { shrink: true, sx: labelSx },
                sx: dateFieldSx,
              },
            }}
          />
        </Box>
      </Box>

      <div className="filter-field">
        <label htmlFor="search">Arama</label>
        <input
          id="search"
          type="text"
          placeholder="Başlık ve içerikte ara..."
          value={localState.search ?? ''}
          onChange={(e) => handleFieldChange('search', e.target.value || undefined)}
        />
      </div>

      <button className="filter-submit" type="button" onClick={onSubmit}>
        Filtrele
      </button>
    </div>
  );
}

export default FilterPanel;
