class Epicurve2 {
    constructor(svg, width, height, xScale, yScale, xAxis, yAxis, yScale2, yAxis2) {
        this.svg = svg;
        this.width = width;
        this.height = height;
        this.xScale = xScale;
        this.yScale = yScale;
        this.xAxis = xAxis;
        this.yAxis = yAxis;
        this.yScale2 = yScale2;
        this.yAxis2 = yAxis2;
    }

    updateBarChart(data, xCallback, yCallback, clazz, color, highlightColor) {
        let selectedData = this.svg.selectAll("." + clazz).data(data);
        let enterData = selectedData.enter();

        enterData.append("rect")
            .style("shape-rendering", "crispEdges")
            .attr("class", clazz)
            .attr("x", d => this.xScale(xCallback(d)))
            .attr("y", d => yCallback(d) > 0 ? this.yScale(yCallback(d)) : this.yScale(0))
            .attr("height", d => Math.abs(this.yScale(yCallback(d)) - this.yScale(0)))
            .attr("width", this.xScale.bandwidth())
            .style("fill", d => yCallback(d) > 0 ? color : highlightColor);

        selectedData.merge(selectedData).transition().duration(100)
            .attr("x", d => this.xScale(xCallback(d)))
            .attr("y", d => yCallback(d) > 0 ? this.yScale(yCallback(d)) : this.yScale(0))
            .attr("height", d => Math.abs(this.yScale(yCallback(d)) - this.yScale(0)))
            .style("fill", d => yCallback(d) > 0 ? color : highlightColor);

        selectedData.exit().remove();

        // Ensure notable dates lines are on top!
        this.svg.selectAll("." + clazz).moveToFront();
        this.svg.selectAll("line").moveToFront();
        this.svg.selectAll('.mouseoverclazz').moveToFront();
    }
}

function getCountyFromUrl(url) {
    let selectedCounty = new URLSearchParams(url).get("county");

    if (!selectedCounty) {
        selectedCounty = "georgia";
    }

    return selectedCounty;
}

function constructCountyUrl(type, county) {
    return "reports/v2/csv/" + county + "/" + type + "_" + county + ".csv"
}

function parseLagData(summaryData, caseDeltaData, caseHistData, caseHistCumData) {
    let tempChartData = [];
    let filteredSummaryData = summaryData.filter(d => d.reportDate >= "2020-05-11");

    for (let i = 0; i < caseHistData.length; i++) {
        let currentCaseHistData = caseHistData[i];
        let currentCaseHistCumData = caseHistCumData[i];
        let currentReportDateString = currentCaseHistData["reportDate"];
        let currentReportDateObj = new Date(currentReportDateString);
        currentReportDateObj.setDate(new Date(currentReportDateString).getDate() + 1);
        let currentSummary = filteredSummaryData.filter(d => d.reportDate === currentReportDateString)[0];
        let currentId = currentSummary.id;
        let currentDeltaData = caseDeltaData.filter(d => d.id === currentId)[0];

        let currentTimeseries = tempChartData[currentId];
        if (!currentTimeseries) {
            currentTimeseries = [];
            tempChartData[currentId] = currentTimeseries;
        }

        for (let j = 0; j < 100; j++) {
            let currentLabelDate = getFormattedDate(currentReportDateObj);
            currentTimeseries.push({
                label: currentLabelDate,
                casesHist: +currentCaseHistData[j],
                casesHistCum: +currentCaseHistCumData[j],
                casesDelta: +currentDeltaData[currentLabelDate],
            });
            currentReportDateObj.setDate(currentReportDateObj.getDate() - 1);
        }
    }

    return tempChartData;
}

function applyDateOffset(date, offset) {
    let dateObj = convertDate(date);
    dateObj.setDate(dateObj.getDate() - offset);
    return getFormattedDate(dateObj);
}

function getFormattedDate(date) {
    let year = date.getFullYear();
    let month = (1 + date.getMonth()).toString().padStart(2, '0');
    let day = date.getDate().toString().padStart(2, '0');

    return year + '-' + month + '-' + day;
}

/**
 * Convert a date string in 2020-10-10T101300 format into a Date() object.
 */
function convertDate(dateString) {
    let filteredDateString = dateString.slice(0, 15) + ":" + dateString.slice(15 + Math.abs(0))
    filteredDateString = filteredDateString.slice(0, 13) + ":" + filteredDateString.slice(13 + Math.abs(0));
    filteredDateString = filteredDateString.replace("T", " ");
    return new Date(filteredDateString);
}

function createSlider(data) {
    let count = 0;

    for (let value of Object.values(data)) {
        if (value.length > 0) {
            count++
        }
    }

    count = (count - 1) * -1;

    let tickValueArray = [];

    for (let current = 0; current > count; current = current - 7) {
        tickValueArray.push(current);
    }

    let sliderTime = d3
        .sliderBottom()
        //.min(-1 * (timeData.length - 1))
        //.max(0)
        .domain([count, 0])
        .marks([count, 0])
        .step(1)
        //.ticks(5)
        .width(350)
        .tickValues(tickValueArray);

    var gTime = d3
        .select('div#slider-time')
        .append('svg')
        .attr('width', 400)
        .attr('height', 100)
        .append('g')
        .attr('transform', 'translate(30,30)');

    gTime.call(sliderTime);

    return sliderTime;
}

function getLastElement(data) {
    let keySet = Object.keys(data);
    return data[keySet[keySet.length - 1]];
}