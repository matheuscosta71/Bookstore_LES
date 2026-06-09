import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
} from 'recharts';
import type { SalesCategoryVolumeChart as TChart } from '@/types/api';

type Props = {
  data: TChart;
  selectedCategories: string[];
};

const COLORS = [
  '#1e3a8a', // Dark Blue
  '#c2410c', // Dark Orange/Rust
  '#0f766e', // Teal
  '#6b21a8', // Purple
  '#b91c1c', // Dark Red
  '#15803d', // Green
];

export function SalesCategoryVolumeLineChart({ data, selectedCategories }: Props) {
  const chartData = data.labels.map((label, i) => {
    const point: Record<string, any> = { name: label };
    data.series.forEach((s) => {
      point[s.category] = s.volumes[i] ?? 0;
    });
    return point;
  });

  return (
    <div className="h-80 w-full" id="sales-category-volume-chart-container">
      <ResponsiveContainer width="100%" height="100%">
        <LineChart data={chartData} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="#e7e5e4" />
          <XAxis dataKey="name" tick={{ fontSize: 11 }} stroke="#78716c" />
          <YAxis
            tick={{ fontSize: 11 }}
            stroke="#78716c"
            allowDecimals={false}
            tickFormatter={(v) => String(v)}
          />
          <Tooltip
            formatter={(value: number, name: string) => [`${value} un.`, name]}
            labelFormatter={(l) => `Mês: ${l}`}
            contentStyle={{ borderRadius: 8, border: '1px solid #e7e5e4' }}
          />
          <Legend wrapperStyle={{ fontSize: 12, paddingTop: 10 }} />
          {data.series
            .filter((s) => selectedCategories.includes(s.category))
            .map((s, idx) => (
              <Line
                key={s.category}
                type="monotone"
                dataKey={s.category}
                stroke={COLORS[idx % COLORS.length]}
                strokeWidth={2}
                dot={{ r: 3 }}
                activeDot={{ r: 5 }}
                connectNulls
              />
            ))}
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}
