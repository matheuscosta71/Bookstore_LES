import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from 'recharts';
import { formatBRL } from '@/utils/format';
import type { SalesLineChart as TChart } from '@/types/api';

type Props = {
  data: TChart;
};

export function SalesLineChart({ data }: Props) {
  const chartData = data.labels.map((label, i) => ({
    name: label,
    value: Number(data.values[i] ?? 0),
  }));

  return (
    <div className="h-80 w-full">
      <ResponsiveContainer width="100%" height="100%">
        <LineChart data={chartData} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="#e7e5e4" />
          <XAxis dataKey="name" tick={{ fontSize: 11 }} stroke="#78716c" />
          <YAxis
            tick={{ fontSize: 11 }}
            stroke="#78716c"
            tickFormatter={(v) =>
              new Intl.NumberFormat('pt-BR', { notation: 'compact', compactDisplay: 'short' }).format(
                v as number,
              )
            }
          />
          <Tooltip
            formatter={(value: number) => formatBRL(value)}
            labelFormatter={(l) => String(l)}
            contentStyle={{ borderRadius: 8, border: '1px solid #e7e5e4' }}
          />
          <Line
            type="monotone"
            dataKey="value"
            stroke="#1e3a5f"
            strokeWidth={2}
            dot={{ r: 3, fill: '#c45c26' }}
            activeDot={{ r: 5 }}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}
