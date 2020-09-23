import { SvgChart, helper } from 'd3kit';
import { scaleLinear, scaleOrdinal, schemeCategory10 } from 'd3-scale';
import { axisLeft, axisBottom } from 'd3-axis';
import { extent } from 'd3-array';

class EpicurveChart extends SvgChart {
    // Define default options for this chart
    static getDefaultOptions() {
        return helper.deepExtend(
            super.getDefaultOptions(),
            {
                margin: {top: 60, right: 60, bottom: 60, left: 60},
                initialWidth: 800,
                initialHeight: 460
            }
        );
    }

    /**
     * Define the names of custom events that can be dispatched from this chart
     * @return {Array[String]} event names
     */
    static getCustomEventNames() {
        return ['bubbleClick'];
    }

    constructor(selector, options) {
        super(selector, options);

        // Add custom variables
        this.xScale = scaleLinear();
        this.yScale = scaleLinear();
        this.color = scaleOrdinal(schemeCategory10);
        this.xAxis = axisBottom().scale(this.xScale);
        this.yAxis = axisLeft().scale(this.yScale);
        this.xAxisG = this.rootG.append('g');
        this.yAxisG = this.rootG.append('g');

        // Add basic event listeners
        this.visualize = this.visualize.bind(this);
        this.on('resize.default', this.visualize);
        this.on('data.default', this.visualize);
    }

    // You can define a new function for this class.
    visualize() {
        if(!this.hasData()) return;

        const data = this.data();

        this.xScale.domain(extent(data, d => d.x))
            .range([0, this.getInnerWidth()]);
        this.yScale.domain(extent(data, d => d.y))
            .range([this.getInnerHeight(), 0]);

        this.xAxisG
            .attr('transform', `translate(0,${this.getInnerHeight()})`)
            .call(this.xAxis);

        this.yAxisG.call(this.yAxis);

        const selection = this.rootG.selectAll('circle')
            .data(data);

        selection.exit().remove();

        const sEnter = selection.enter().append('circle')
            .attr('cx', d => this.xScale(d.x))
            .attr('cy', d => this.yScale(d.y))
            .on('click', (...args) => {
                this.dispatcher.apply('bubbleClick', this, args);
            });

        selection.merge(sEnter)
            .attr('cx', d => this.xScale(d.x))
            .attr('cy', d => this.yScale(d.y))
            .attr('r', d => d.r)
            .style('fill', (d,i) => this.color(i));
    }
}

export default EpicurveChart;