

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

    let gTime = d3
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

/**
 * Gets the value from the provided array as if it were indexed from the end. So 0 gets the last element, and -data.length-1 gets the first element.
 * -1 Gets the next to the last element.
 */
function getReverseIdxValue(data, idx) {
    return data[data.length - 1 + idx];
}

function getLegendHeight(height, size, i) {
    return height + 81 + (i * ((size + 2) + 5))
}

function toggleVisibilityExclusive(d) {
    toggleVisibility(d, true);
}

function toggleVisibility(d) {
    d.clazz.forEach(c => {
        let selection = d3.selectAll("." + c);
        if (selection && selection.data() && selection.data().length > 0) {
            let currentVis = d3.selectAll("." + c).style("visibility");
            console.log("Legend click! Toggling " + d.key + "... current visibility: " + currentVis);
            let targetVis = "visible";
            if (targetVis === currentVis) {
                targetVis = "hidden";
            }

            d3.selectAll("." + c).transition().duration(200).style("visibility", targetVis);
        }
    });
}

function handleDeathMouseOver(d, i, d3This, color) {
    d3This
        .transition()
        .duration(50)
        .style("fill", color)
        .attr("opacity", "0.2");
    tip.show(d, i);
}

function handleDeathMouseOut(d, i, d3This, color) {
    d3This
        .transition()
        .duration(200)
        .style("fill", color)
        .attr("opacity", "0");
    tip.hide(d, i);
}

function dispatchMouseEvents(clazz, scaledX, prevNode, currentNode) {
    currentNode[clazz] = d3.selectAll("." + clazz).filter(d => d.label === scaledX);

    // First time entering?
    if (!prevNode[clazz] && currentNode[clazz]) {
        //console.log("First time? scaledX: " + scaledX);
        prevNode[clazz] = currentNode[clazz];
        currentNode[clazz].dispatch("click");
    }

    // Exited past the range of available data
    if (!currentNode[clazz].data()[0] || currentNode[clazz].data().length === 0) {
        //console.log("Exit past available? prevLabel: " + prevNode[clazz].data()[0].label + " scaledX: " + scaledX);
        prevNode[clazz].dispatch("mouseout");
    }

    // Most common case, moving from 1 good date to the next.
    if ((prevNode[clazz].data().length > 0 && currentNode[clazz].data().length) && (prevNode[clazz].data()[0].label !== currentNode[clazz].data()[0].label)) {
        //console.log("Good transition? prevLabel: " + prevNode[clazz].data()[0].label + " currLabel: " + currentNode[clazz].data()[0].label + " scaledX: " + scaledX);
        prevNode[clazz].dispatch("mouseout");
        currentNode[clazz].dispatch("click");
        prevNode[clazz] = currentNode[clazz];
    } else {
        //console.log("Doing nothing! prevLabel: " + prevNode[clazz].data()[0].label + " scaledX: " + scaledX);
    }
}

function sumTimeSeries(data) {
    return Object.values(data).map(d => +d).filter(d => !isNaN(d)).reduce((d1, d2) => d1 + d2)
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

function getFilteredCountyName(inputCounty) {
    let filteredCounties = allCounties.filter(county => county.toLowerCase() === inputCounty.toLowerCase());
    if (filteredCounties && filteredCounties.length === 1) {
        return filteredCounties[0];
    } else {
        return undefined;
    }
}

const earliestDate = new Date("2020-05-14");

function getDateAtMouse(mouse, xScale) {
    let index = Math.floor(((mouse[0] - xScale(xScale.domain()[0])) / xScale.step()));
    let scaledX = xScale.domain()[index];
    if (!scaledX) {
        scaledX = xScale.domain()[xScale.domain().length - 1];
    }

    //console.log("scaledX " + scaledX + " @ mouse " + mouse[0]);

    return scaledX;
}

function handleDblClick(scaledX, targetFocus) {
    let targetDate = scaledX;

    if (new Date(scaledX) < earliestDate) {
        targetDate = "2020-05-13";
    }

    let event = new CustomEvent('newFocusEvent', {detail: {label: targetDate, focus: targetFocus}});

    dispatchEvent(event);
}

function applyDateOffset(date, offset) {
    let dateObj = this.convertDate(date);
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