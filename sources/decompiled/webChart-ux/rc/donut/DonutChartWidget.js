/**
 * @copyright 2019 Tridium, Inc. All Rights Reserved.
 * @author Tony Richards
 */

/**
 * API Status: **Private**
 * @module nmodule/webChart/rc/donut/DonutChartWidget
 */
define(['d3', 'nmodule/webEditors/rc/fe/BaseWidget'], function (d3, BaseWidget) {
  'use strict';

  var DEFAULT_WIDTH = 140,
      DEFAULT_HEIGHT = 140,
      DEFAULT_RADIUS = 65,
      DEFAULT_STROKE = 6,
      DEFAULT_PAD_ANGLE = 0.04,
      DEFAULT_MARGIN = {
    top: 0,
    right: 0,
    bottom: 0,
    left: 0
  },
      DEFAULT_COLOR = {
    'healthy': '#7eb338',
    'warning': '#ffc627',
    'critical': '#ee3224'
  },
      DEFAULT_COUNT_Y_OFFSET = -15,
      DEFAULT_LABEL_Y_OFFSET = 20,
      DEFAULT_LABEL = '';
  /**
   * Donut chart widget
   * 
   * This class supports the following properties
   * 
   * - `width`: (number) width of the donut chart canvas, defaults to 140
   * - `height`: (number) height of the donut chart canvas, defaults to 140
   * - `radius`: (number) radius of the donut chart, defaults to 65
   * - `stroke`: (number) stroke width of the donut chart, defaults to 6
   * - `padAngle`: (number) padding between each pie portion (in radians)
   *   defaults to 0.04
   * - `margin`: (Object)
   * - `margin.top`: (number) top margin defaults to 0
   * - `margin.right`: (number) right margin defaults to 0
   * - `margin.bottom`: (number) bottom margin defaults to 0
   * - `margin.left`: (number) left margin defaults to 0
   * - `color`: (object) color map, which maps a type specified
   *   in the entries passsed to doLoad() to a color.
   * - `label`: (string) label inside the chart
   * - `labelYOffset`: (number) y offset of the label, defaults to 20
   * - `countYOffset`: (number) y offset of the count, defaults to -15
   * 
   * @example
   *   <caption>Create a donut chart 1 critical, 2 warning, 3 healthy 
   * features</caption>
   * <body class="ux-root">
   * <div class="widgetGoesHere"></div>
   * </body>
   * <script>
   *    return fe.buildFor({
   *      dom: $('.widgetGoesHere'),
   *      formFactor: 'mini',
   *      properties: { label: 'TOTAL FEATURES',
   *        color: {
   *          'healthy': '#7eb338',
   *          'warning': '#ffc627',
   *          'critical': '#ee3224'
   *        }
   *      },
   *      type: DonutChartWidget,
   *      value: [
   *        { type: 'healthy', value: 3 },
   *        { type: 'warning', value: 2 },
   *        { type: 'critical', value: 1 }
   *      ]
   *    });
   *
   * @class
   * @alias module:nmodule/webChart/rc/donut/DonutChartWidget
   * @extends module:nmodule/webEditors/rc/fe/BaseWidget
   */

  var DonutChartWidget = function DonutChartWidget(params) {
    BaseWidget.apply(this, arguments);
  };

  DonutChartWidget.prototype = Object.create(BaseWidget.prototype);
  DonutChartWidget.prototype.constructor = DonutChartWidget;
  /**
   * Initialize the Donut Chart
   * 
   * @param {JQuery} dom
   */

  DonutChartWidget.prototype.doInitialize = function (dom) {
    this.$canvas = d3.select(dom[0]).append('svg');
    this.$group = this.$canvas.append('g');
    this.$pieLayout = d3.layout.pie().value(function (d) {
      return d.value;
    }).sort(null);
    this.$label1 = this.$group.append('text').attr('class', 'donut-label-1').attr('y', this.countYOffset).attr('text-anchor', 'middle').attr('font-size', '55px');
    this.$label2 = this.$group.append('text').attr('class', 'donut-label-2').attr('y', this.labelYOffset).attr('text-anchor', 'middle').attr('font-size', '12px');
    return this.$rebuildHtml();
  };
  /**
   * Load an array of values for the Donut Chart.
   * 
   * @param {Array.<{ type: string, value: number}>} values
   */


  DonutChartWidget.prototype.doLoad = function (values) {
    var that = this;
    that.$data = values;
    return that.$rebuildHtml();
  };

  DonutChartWidget.prototype.$rebuildHtml = function () {
    var that = this;
    that.$canvas.attr('width', that.canvasWidth).attr('height', that.canvasHeight);
    that.$group.attr('transform', 'translate(' + (that.margin.left + that.width / 2) + ',' + (that.margin.top + that.height / 2) + ')');
    that.$label2.text(this.label);

    if (that.$arcs) {
      that.$arcs.remove();
    }

    that.$arcRenderer = d3.svg.arc().innerRadius(that.radius - that.stroke).outerRadius(that.radius).cornerRadius(that.stroke).padAngle(that.padAngle);

    if (that.$data) {
      that.$label1.text(that.$data.reduce(function (prev, current) {
        return prev + current.value;
      }, 0));
      that.$arcs = that.$group.selectAll('.arc').data(that.$pieLayout(that.$data)).enter().append('g').attr('class', 'arc');
      that.$arcs.append('path').attr('d', that.$arcRenderer).attr('fill', function (d) {
        return that.color[d.data.type];
      });
    }
  };
  /**
   * width property
   */


  Object.defineProperty(DonutChartWidget.prototype, 'width', {
    get: function get() {
      return this.properties().getValue('width') || DEFAULT_WIDTH;
    }
  });
  /**
   * height property
   */

  Object.defineProperty(DonutChartWidget.prototype, 'height', {
    get: function get() {
      return this.properties().getValue('height') || DEFAULT_HEIGHT;
    }
  });
  /**
   * radius property
   */

  Object.defineProperty(DonutChartWidget.prototype, 'radius', {
    get: function get() {
      return this.properties().getValue('radius') || DEFAULT_RADIUS;
    }
  });
  /**
   * stroke property
   */

  Object.defineProperty(DonutChartWidget.prototype, 'stroke', {
    get: function get() {
      return this.properties().getValue('stroke') || DEFAULT_STROKE;
    }
  });
  /**
   * padAngle property
   */

  Object.defineProperty(DonutChartWidget.prototype, 'padAngle', {
    get: function get() {
      var angle = this.properties().getValue('padAngle');

      if (typeof angle === 'number') {
        return angle;
      }

      return DEFAULT_PAD_ANGLE;
    }
  });
  /**
   * margin property
   */

  Object.defineProperty(DonutChartWidget.prototype, 'margin', {
    get: function get() {
      return this.properties().getValue('margin') || DEFAULT_MARGIN;
    }
  });
  /**
   * label property
   */

  Object.defineProperty(DonutChartWidget.prototype, 'label', {
    get: function get() {
      return this.properties().getValue('label') || DEFAULT_LABEL;
    }
  });
  /**
   * color property
   */

  Object.defineProperty(DonutChartWidget.prototype, 'color', {
    get: function get() {
      return this.properties().getValue('color') || DEFAULT_COLOR;
    }
  });
  /**
   * canvasWidth property
   */

  Object.defineProperty(DonutChartWidget.prototype, 'canvasWidth', {
    get: function get() {
      return this.width - this.margin.left - this.margin.right;
    }
  });
  /**
   * canvasHeight property
   */

  Object.defineProperty(DonutChartWidget.prototype, 'canvasHeight', {
    get: function get() {
      return this.height - this.margin.top - this.margin.bottom;
    }
  });
  /**
   * countYOffset property
   */

  Object.defineProperty(DonutChartWidget.prototype, 'countYOffset', {
    get: function get() {
      var countYOffset = this.properties().getValue('countYOffset');

      if (typeof countYOffset === 'number') {
        return countYOffset;
      }

      return DEFAULT_COUNT_Y_OFFSET;
    }
  });
  /**
   * labelYOffset property
   */

  Object.defineProperty(DonutChartWidget.prototype, 'labelYOffset', {
    get: function get() {
      var labelYOffset = this.properties().getValue('labelYOffset');

      if (typeof labelYOffset === 'number') {
        return labelYOffset;
      }

      return DEFAULT_LABEL_Y_OFFSET;
    }
  });
  return DonutChartWidget;
});
